package com.showhop.api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.showhop.api.config.WebhookProperties;
import com.showhop.api.entity.WebhookDelivery;
import com.showhop.api.entity.WebhookEndpoint;
import com.showhop.api.entity.WebhookEvent;
import com.showhop.api.entity.enums.WebhookDeliveryState;
import com.showhop.api.entity.enums.WebhookEndpointStatus;
import com.showhop.api.entity.enums.WebhookEventType;
import com.showhop.api.repository.WebhookDeliveryRepository;
import com.showhop.api.service.impl.WebhookDeliveryWorker;
import com.showhop.api.service.impl.WebhookSigner;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.micrometer.tracing.test.simple.SimpleTracer;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

@org.junit.jupiter.api.extension.ExtendWith(MockitoExtension.class)
class WebhookDeliveryWorkerTest {

  @Mock
  private WebhookDeliveryRepository webhookDeliveryRepository;

  private MockRestServiceServer mockServer;
  private WebhookDeliveryWorker worker;
  private WebhookProperties properties;

  @BeforeEach
  void setUp() {
    RestClient.Builder builder = RestClient.builder();
    mockServer = MockRestServiceServer.bindTo(builder).build();
    properties = new WebhookProperties(0, 20, Duration.ofMinutes(2), 3,
        Duration.ofSeconds(30), Duration.ofHours(1), 2, Duration.ofMinutes(5));
    worker = new WebhookDeliveryWorker(
        webhookDeliveryRepository, builder.build(), new ObjectMapper(), properties, new WebhookSigner(),
        new SimpleMeterRegistry(), new SimpleTracer());
  }

  @Test
  void aSuccessfulDeliveryIsMarkedSucceededAndResetsEndpointFailures() {
    WebhookEndpoint endpoint = anEndpoint(WebhookEndpointStatus.ACTIVE, 3);
    WebhookDelivery delivery = aDelivery(endpoint, false);
    when(webhookDeliveryRepository.findClaimable(anyInt())).thenReturn(List.of(delivery));
    when(webhookDeliveryRepository.findById(delivery.getId())).thenReturn(Optional.of(delivery));

    mockServer.expect(requestTo(endpoint.getUrl()))
        .andExpect(header("Idempotency-Key", delivery.getId().toString()))
        .andRespond(withSuccess());

    worker.pollAndDeliver();
    mockServer.verify();

    assertThat(delivery.getState()).isEqualTo(WebhookDeliveryState.SUCCEEDED);
    assertThat(endpoint.getConsecutiveFailures()).isZero();
  }

  @Test
  void aFailedDeliveryBelowMaxAttemptsIsScheduledForRetryWithBackoff() {
    WebhookEndpoint endpoint = anEndpoint(WebhookEndpointStatus.ACTIVE, 3);
    WebhookDelivery delivery = aDelivery(endpoint, false);
    delivery.setAttempt(0); // about to become attempt 1 of 3
    when(webhookDeliveryRepository.findClaimable(anyInt())).thenReturn(List.of(delivery));
    when(webhookDeliveryRepository.findById(delivery.getId())).thenReturn(Optional.of(delivery));

    mockServer.expect(requestTo(endpoint.getUrl()))
        .andRespond(withServerError());

    worker.pollAndDeliver();

    assertThat(delivery.getState()).isEqualTo(WebhookDeliveryState.RETRYING);
    assertThat(delivery.getNextRetryAt()).isAfter(Instant.now());
    assertThat(endpoint.getConsecutiveFailures()).isEqualTo(1);
  }

  @Test
  void aFailedDeliveryAtMaxAttemptsIsDeadLettered() {
    WebhookEndpoint endpoint = anEndpoint(WebhookEndpointStatus.ACTIVE, 3);
    WebhookDelivery delivery = aDelivery(endpoint, false);
    delivery.setAttempt(2); // about to become attempt 3 of 3 -- the last one
    when(webhookDeliveryRepository.findClaimable(anyInt())).thenReturn(List.of(delivery));
    when(webhookDeliveryRepository.findById(delivery.getId())).thenReturn(Optional.of(delivery));

    mockServer.expect(requestTo(endpoint.getUrl()))
        .andRespond(withServerError());

    worker.pollAndDeliver();

    assertThat(delivery.getState()).isEqualTo(WebhookDeliveryState.DEAD_LETTER);
  }

  @Test
  void enoughConsecutiveFailuresOpensTheCircuit() {
    WebhookEndpoint endpoint = anEndpoint(WebhookEndpointStatus.ACTIVE, 3);
    endpoint.setConsecutiveFailures(1); // one below the threshold of 2
    WebhookDelivery delivery = aDelivery(endpoint, false);
    when(webhookDeliveryRepository.findClaimable(anyInt())).thenReturn(List.of(delivery));
    when(webhookDeliveryRepository.findById(delivery.getId())).thenReturn(Optional.of(delivery));

    mockServer.expect(requestTo(endpoint.getUrl())).andRespond(withServerError());

    worker.pollAndDeliver();

    assertThat(endpoint.getStatus()).isEqualTo(WebhookEndpointStatus.CIRCUIT_OPEN);
    assertThat(endpoint.getCircuitOpenedAt()).isNotNull();
  }

  @Test
  void aSuccessfulProbeClosesTheCircuit() {
    WebhookEndpoint endpoint = anEndpoint(WebhookEndpointStatus.CIRCUIT_OPEN, 3);
    endpoint.setCircuitOpenedAt(Instant.now().minus(Duration.ofMinutes(10)));
    WebhookDelivery probe = aDelivery(endpoint, true);
    when(webhookDeliveryRepository.findClaimable(anyInt())).thenReturn(List.of(probe));
    when(webhookDeliveryRepository.findById(probe.getId())).thenReturn(Optional.of(probe));

    mockServer.expect(requestTo(endpoint.getUrl())).andRespond(withSuccess());

    worker.pollAndDeliver();

    assertThat(probe.getState()).isEqualTo(WebhookDeliveryState.SUCCEEDED);
    assertThat(endpoint.getStatus()).isEqualTo(WebhookEndpointStatus.ACTIVE);
    assertThat(endpoint.getCircuitOpenedAt()).isNull();
  }

  @Test
  void aFailedProbeStaysOpenAndRestartsTheCooldownInsteadOfRetrying() {
    WebhookEndpoint endpoint = anEndpoint(WebhookEndpointStatus.CIRCUIT_OPEN, 3);
    Instant originalOpenedAt = Instant.now().minus(Duration.ofMinutes(10));
    endpoint.setCircuitOpenedAt(originalOpenedAt);
    WebhookDelivery probe = aDelivery(endpoint, true);
    when(webhookDeliveryRepository.findClaimable(anyInt())).thenReturn(List.of(probe));
    when(webhookDeliveryRepository.findById(probe.getId())).thenReturn(Optional.of(probe));

    mockServer.expect(requestTo(endpoint.getUrl())).andRespond(withServerError());

    worker.pollAndDeliver();

    assertThat(probe.getState()).isEqualTo(WebhookDeliveryState.DEAD_LETTER);
    assertThat(endpoint.getStatus()).isEqualTo(WebhookEndpointStatus.CIRCUIT_OPEN);
    assertThat(endpoint.getCircuitOpenedAt()).isAfter(originalOpenedAt);
  }

  private WebhookEndpoint anEndpoint(WebhookEndpointStatus status, int maxAttempts) {
    return WebhookEndpoint.builder()
        .id(UUID.randomUUID()).organizerId(UUID.randomUUID())
        .url("https://example.com/hooks/" + UUID.randomUUID())
        .secret("whsec_test").subscribedEventTypes(List.of("event.published"))
        .status(status).consecutiveFailures(0).build();
  }

  private WebhookDelivery aDelivery(WebhookEndpoint endpoint, boolean probe) {
    WebhookEvent event = WebhookEvent.builder()
        .id(UUID.randomUUID()).organizerId(endpoint.getOrganizerId())
        .type(WebhookEventType.EVENT_PUBLISHED)
        .payload(Map.of("eventId", "abc")).occurredAt(Instant.now()).build();
    return WebhookDelivery.builder()
        .id(UUID.randomUUID()).endpoint(endpoint).event(event)
        .state(WebhookDeliveryState.PENDING).attempt(0).maxAttempts(3).probe(probe).build();
  }
}
