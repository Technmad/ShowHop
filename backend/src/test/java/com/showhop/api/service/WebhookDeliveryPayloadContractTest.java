package com.showhop.api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.fasterxml.jackson.databind.JsonNode;
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
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpMethod;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.client.RequestMatcher;
import org.springframework.web.client.RestClient;

/**
 * Pins the actual bytes-on-the-wire contract a receiver depends on: the
 * envelope shape ({@code {"type": ..., "data": {...}}}), which headers
 * carry the signature/timestamp/idempotency key, and that the signature is
 * a genuine, independently-recomputable HMAC over exactly what was sent --
 * not just "some string in a header." A receiver integration built against
 * this shape should never break silently.
 */
@org.junit.jupiter.api.extension.ExtendWith(MockitoExtension.class)
class WebhookDeliveryPayloadContractTest {

  @Mock
  private WebhookDeliveryRepository webhookDeliveryRepository;

  private MockRestServiceServer mockServer;
  private WebhookDeliveryWorker worker;
  private final ObjectMapper objectMapper = new ObjectMapper();
  private final WebhookSigner signer = new WebhookSigner();

  @BeforeEach
  void setUp() {
    RestClient.Builder builder = RestClient.builder();
    mockServer = MockRestServiceServer.bindTo(builder).build();
    worker = new WebhookDeliveryWorker(
        webhookDeliveryRepository, builder.build(), objectMapper,
        new WebhookProperties(0, 20, Duration.ofMinutes(2), 8,
            Duration.ofSeconds(30), Duration.ofHours(1), 5, Duration.ofMinutes(5)),
        signer, new SimpleMeterRegistry());
  }

  @Test
  void theWireEnvelopeIsExactlyTypeAndDataAndTheSignatureVerifiesIndependently() throws Exception {
    String secret = "whsec_contract_test";
    WebhookEndpoint endpoint = WebhookEndpoint.builder()
        .id(UUID.randomUUID()).organizerId(UUID.randomUUID())
        .url("https://example.com/hooks").secret(secret)
        .subscribedEventTypes(List.of("event.published"))
        .status(WebhookEndpointStatus.ACTIVE).build();
    WebhookEvent event = WebhookEvent.builder()
        .id(UUID.randomUUID()).organizerId(endpoint.getOrganizerId())
        .type(WebhookEventType.EVENT_PUBLISHED)
        .payload(Map.of("eventId", "abc-123", "name", "Autumn Tech Meetup", "venue", "Riverside Hall"))
        .occurredAt(Instant.now()).build();
    WebhookDelivery delivery = WebhookDelivery.builder()
        .id(UUID.randomUUID()).endpoint(endpoint).event(event)
        .state(WebhookDeliveryState.PENDING).attempt(0).maxAttempts(8).build();

    when(webhookDeliveryRepository.findClaimable(anyInt())).thenReturn(List.of(delivery));
    when(webhookDeliveryRepository.findById(delivery.getId())).thenReturn(Optional.of(delivery));

    AtomicReference<String> capturedBody = new AtomicReference<>();
    AtomicReference<String> capturedSignature = new AtomicReference<>();
    AtomicReference<String> capturedTimestamp = new AtomicReference<>();
    AtomicReference<String> capturedIdempotencyKey = new AtomicReference<>();

    mockServer.expect(requestTo(endpoint.getUrl()))
        .andExpect(method(HttpMethod.POST))
        .andExpect((RequestMatcher) request -> {
          var mockRequest = (org.springframework.mock.http.client.MockClientHttpRequest) request;
          capturedBody.set(mockRequest.getBodyAsString());
          capturedSignature.set(request.getHeaders().getFirst("Webhook-Signature"));
          capturedTimestamp.set(request.getHeaders().getFirst("Webhook-Timestamp"));
          capturedIdempotencyKey.set(request.getHeaders().getFirst("Idempotency-Key"));
        })
        .andRespond(withSuccess());

    worker.pollAndDeliver();
    mockServer.verify();

    JsonNode envelope = objectMapper.readTree(capturedBody.get());
    assertThat(fieldNames(envelope)).containsExactlyInAnyOrder("type", "data");
    assertThat(envelope.get("type").asText()).isEqualTo("event.published");
    assertThat(envelope.get("data").get("eventId").asText()).isEqualTo("abc-123");
    assertThat(envelope.get("data").get("name").asText()).isEqualTo("Autumn Tech Meetup");
    assertThat(envelope.get("data").get("venue").asText()).isEqualTo("Riverside Hall");

    assertThat(capturedIdempotencyKey.get()).isEqualTo(delivery.getId().toString());
    assertThat(capturedTimestamp.get()).matches("\\d+");

    // The receiver-side verification story: recompute the signature with
    // only the shared secret, the timestamp header, and the raw body --
    // exactly what a real integrator's code would have available.
    String expectedSignature = signer.sign(secret, capturedTimestamp.get(), capturedBody.get());
    assertThat(capturedSignature.get()).isEqualTo(expectedSignature);
  }

  private java.util.List<String> fieldNames(JsonNode node) {
    java.util.List<String> names = new java.util.ArrayList<>();
    node.fieldNames().forEachRemaining(names::add);
    return names;
  }
}
