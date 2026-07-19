package com.showhop.api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.showhop.api.entity.User;
import com.showhop.api.entity.WebhookDelivery;
import com.showhop.api.entity.WebhookEndpoint;
import com.showhop.api.entity.WebhookEvent;
import com.showhop.api.entity.enums.WebhookDeliveryState;
import com.showhop.api.entity.enums.WebhookEndpointStatus;
import com.showhop.api.entity.enums.WebhookEventType;
import com.showhop.api.repository.UserRepository;
import com.showhop.api.repository.WebhookDeliveryRepository;
import com.showhop.api.repository.WebhookEndpointRepository;
import com.showhop.api.repository.WebhookEventRepository;
import com.showhop.api.service.impl.WebhookDeliveryWorker;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;

/**
 * Proves a real, Spring-managed {@code webhookDeliveryWorker} bean's
 * {@code pollAndDeliver()} -- called externally, exactly as
 * {@code WebhookScheduler} calls it -- actually persists the outcome, not
 * just mutates the in-memory objects it read. Every other
 * WebhookDeliveryWorker test constructs the class directly with {@code new}
 * (bypassing Spring's proxy entirely) and asserts against those same
 * in-memory objects, which can't tell the difference between "persisted"
 * and "mutated a since-detached entity" -- exactly the gap where the
 * self-invocation bug lived (claimBatch/recordOutcome are called from
 * pollAndDeliver via plain Java self-calls, which bypass the
 * {@code @Transactional} proxy entirely; see the Javadoc on
 * WebhookDeliveryWorker.setSelf).
 */
@SpringBootTest(properties = "spring.main.allow-bean-definition-overriding=true")
@Import(WebhookDeliveryWorkerSelfInvocationTest.MockWebhookRestClientConfig.class)
@Transactional
class WebhookDeliveryWorkerSelfInvocationTest {

  @TestConfiguration
  static class MockWebhookRestClientConfig {
    static MockRestServiceServer mockServer;

    // @Primary because bean-definition-overriding replaces the whole
    // original definition -- including its own @Primary -- so without
    // this, webhookRestClient and razorpayRestClient both become
    // non-primary and every unqualified RestClient injection becomes
    // ambiguous again.
    @Bean
    @org.springframework.context.annotation.Primary
    RestClient webhookRestClient(RestClient.Builder builder) {
      mockServer = MockRestServiceServer.bindTo(builder).build();
      return builder.build();
    }
  }

  @Autowired
  private WebhookDeliveryWorker webhookDeliveryWorker;
  @Autowired
  private UserRepository userRepository;
  @Autowired
  private WebhookEndpointRepository webhookEndpointRepository;
  @Autowired
  private WebhookEventRepository webhookEventRepository;
  @Autowired
  private WebhookDeliveryRepository webhookDeliveryRepository;

  @Test
  void pollAndDeliverThroughTheRealBeanActuallyPersistsTheSucceededState() {
    User organizer = userRepository.saveAndFlush(User.builder()
        .id(UUID.randomUUID()).name("Organizer")
        .email("organizer-selfinvoke-" + UUID.randomUUID() + "@example.com").build());
    WebhookEndpoint endpoint = webhookEndpointRepository.saveAndFlush(WebhookEndpoint.builder()
        .organizerId(organizer.getId()).url("https://example.com/hooks/selfinvoke").secret("whsec_test")
        .subscribedEventTypes(List.of("event.published"))
        .status(WebhookEndpointStatus.ACTIVE).build());
    WebhookEvent event = webhookEventRepository.saveAndFlush(WebhookEvent.builder()
        .organizerId(organizer.getId()).type(WebhookEventType.EVENT_PUBLISHED)
        .payload(Map.of("eventId", "abc")).occurredAt(Instant.now()).build());
    WebhookDelivery delivery = webhookDeliveryRepository.saveAndFlush(WebhookDelivery.builder()
        .endpoint(endpoint).event(event).state(WebhookDeliveryState.PENDING)
        .attempt(0).maxAttempts(8).build());

    MockWebhookRestClientConfig.mockServer.expect(requestTo(endpoint.getUrl())).andRespond(withSuccess());

    webhookDeliveryWorker.pollAndDeliver();

    WebhookDelivery reloaded = webhookDeliveryRepository.findById(delivery.getId()).orElseThrow();
    assertThat(reloaded.getState()).isEqualTo(WebhookDeliveryState.SUCCEEDED);
    assertThat(webhookEndpointRepository.findById(endpoint.getId()).orElseThrow().getConsecutiveFailures())
        .isZero();
  }
}
