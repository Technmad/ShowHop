package com.showhop.api.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.showhop.api.entity.User;
import com.showhop.api.entity.WebhookDelivery;
import com.showhop.api.entity.WebhookEndpoint;
import com.showhop.api.entity.WebhookEvent;
import com.showhop.api.entity.enums.WebhookDeliveryState;
import com.showhop.api.entity.enums.WebhookEventType;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.PageRequest;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class WebhookDeliveryRepositoryTest {

  @Autowired private WebhookDeliveryRepository webhookDeliveryRepository;
  @Autowired private WebhookEndpointRepository webhookEndpointRepository;
  @Autowired private WebhookEventRepository webhookEventRepository;
  @Autowired private UserRepository userRepository;

  @Test
  void savesADeliveryAndListsByEndpointNewestFirst() {
    User organizer = userRepository.saveAndFlush(User.builder()
        .id(UUID.randomUUID()).name("Priya").email("priya@example.com").build());
    WebhookEndpoint endpoint = webhookEndpointRepository.saveAndFlush(WebhookEndpoint.builder()
        .organizerId(organizer.getId()).url("https://example.com/hooks").secret("s")
        .subscribedEventTypes(List.of("event.published")).build());
    WebhookEvent event = webhookEventRepository.saveAndFlush(WebhookEvent.builder()
        .organizerId(organizer.getId()).type(WebhookEventType.EVENT_PUBLISHED)
        .payload(Map.of("eventId", "abc")).occurredAt(Instant.now()).build());

    WebhookDelivery delivery = webhookDeliveryRepository.saveAndFlush(WebhookDelivery.builder()
        .endpoint(endpoint).event(event).build());

    var page = webhookDeliveryRepository
        .findByEndpointIdOrderByCreatedAtDesc(endpoint.getId(), PageRequest.of(0, 10));

    assertThat(page.getContent()).extracting(WebhookDelivery::getId).containsExactly(delivery.getId());
    assertThat(delivery.getState()).isEqualTo(WebhookDeliveryState.PENDING);
    assertThat(delivery.getAttempt()).isZero();
    assertThat(delivery.isProbe()).isFalse();
  }

  @Test
  void detectsAnInFlightProbeForAnEndpoint() {
    User organizer = userRepository.saveAndFlush(User.builder()
        .id(UUID.randomUUID()).name("Priya").email("priya2@example.com").build());
    WebhookEndpoint endpoint = webhookEndpointRepository.saveAndFlush(WebhookEndpoint.builder()
        .organizerId(organizer.getId()).url("https://example.com/hooks").secret("s")
        .subscribedEventTypes(List.of("event.published")).build());
    WebhookEvent event = webhookEventRepository.saveAndFlush(WebhookEvent.builder()
        .organizerId(organizer.getId()).type(WebhookEventType.EVENT_PUBLISHED)
        .payload(Map.of("eventId", "abc")).occurredAt(Instant.now()).build());

    webhookDeliveryRepository.saveAndFlush(WebhookDelivery.builder()
        .endpoint(endpoint).event(event).probe(true)
        .state(WebhookDeliveryState.IN_FLIGHT).build());

    boolean hasInFlightProbe = webhookDeliveryRepository.existsByEndpointIdAndProbeTrueAndStateNotIn(
        endpoint.getId(), List.of(WebhookDeliveryState.SUCCEEDED, WebhookDeliveryState.DEAD_LETTER));

    assertThat(hasInFlightProbe).isTrue();
  }
}
