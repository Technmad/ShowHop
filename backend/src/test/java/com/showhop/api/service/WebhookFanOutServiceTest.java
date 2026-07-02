package com.showhop.api.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.showhop.api.config.WebhookProperties;
import com.showhop.api.entity.User;
import com.showhop.api.entity.WebhookEndpoint;
import com.showhop.api.entity.WebhookEvent;
import com.showhop.api.entity.enums.WebhookDeliveryState;
import com.showhop.api.entity.enums.WebhookEndpointStatus;
import com.showhop.api.entity.enums.WebhookEventType;
import com.showhop.api.repository.UserRepository;
import com.showhop.api.repository.WebhookDeliveryRepository;
import com.showhop.api.repository.WebhookEndpointRepository;
import com.showhop.api.repository.WebhookEventRepository;
import com.showhop.api.service.impl.WebhookFanOutService;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class WebhookFanOutServiceTest {

  @Autowired private WebhookEventRepository webhookEventRepository;
  @Autowired private WebhookEndpointRepository webhookEndpointRepository;
  @Autowired private WebhookDeliveryRepository webhookDeliveryRepository;
  @Autowired private UserRepository userRepository;

  private WebhookFanOutService fanOutService;
  private UUID organizerId;

  private void init() {
    fanOutService = new WebhookFanOutService(
        webhookEventRepository, webhookEndpointRepository, webhookDeliveryRepository,
        new WebhookProperties(0, 0, null, 0, null, null, 0, Duration.ofMinutes(5)));
    organizerId = userRepository.saveAndFlush(
        User.builder().id(UUID.randomUUID()).name("Priya").email("priya@example.com").build())
        .getId();
  }

  @Test
  void createsADeliveryOnlyForActiveEndpointsSubscribedToThatEventType() {
    init();
    WebhookEndpoint subscribed = anEndpoint(WebhookEndpointStatus.ACTIVE, "event.published");
    WebhookEndpoint unsubscribed = anEndpoint(WebhookEndpointStatus.ACTIVE, "ticket.purchased");
    WebhookEndpoint disabled = anEndpoint(WebhookEndpointStatus.DISABLED, "event.published");
    WebhookEvent event = anEvent(WebhookEventType.EVENT_PUBLISHED);

    int created = fanOutService.fanOutDueEvents();

    assertThat(created).isEqualTo(1);
    var deliveries = webhookDeliveryRepository
        .findByEndpointIdOrderByCreatedAtDesc(subscribed.getId(), org.springframework.data.domain.PageRequest.of(0, 10));
    assertThat(deliveries.getContent()).hasSize(1);
    assertThat(webhookEventRepository.findById(event.getId()).orElseThrow().getFannedOutAt())
        .isNotNull();
    assertThat(webhookDeliveryRepository
        .findByEndpointIdOrderByCreatedAtDesc(unsubscribed.getId(), org.springframework.data.domain.PageRequest.of(0, 10))
        .getContent()).isEmpty();
    assertThat(webhookDeliveryRepository
        .findByEndpointIdOrderByCreatedAtDesc(disabled.getId(), org.springframework.data.domain.PageRequest.of(0, 10))
        .getContent()).isEmpty();
  }

  @Test
  void doesNotProbeACircuitOpenEndpointBeforeItsCooldownElapses() {
    init();
    WebhookEndpoint stillOpen = webhookEndpointRepository.saveAndFlush(WebhookEndpoint.builder()
        .organizerId(organizerId).url("https://a.example.com").secret("s")
        .subscribedEventTypes(List.of("event.published"))
        .status(WebhookEndpointStatus.CIRCUIT_OPEN)
        .circuitOpenedAt(Instant.now()) // just opened -- cooldown not elapsed
        .build());
    anEvent(WebhookEventType.EVENT_PUBLISHED);

    fanOutService.fanOutDueEvents();

    assertThat(webhookDeliveryRepository
        .findByEndpointIdOrderByCreatedAtDesc(stillOpen.getId(), org.springframework.data.domain.PageRequest.of(0, 10))
        .getContent()).isEmpty();
  }

  @Test
  void probesACircuitOpenEndpointOnceItsCooldownHasElapsed() {
    init();
    WebhookEndpoint coolable = webhookEndpointRepository.saveAndFlush(WebhookEndpoint.builder()
        .organizerId(organizerId).url("https://a.example.com").secret("s")
        .subscribedEventTypes(List.of("event.published"))
        .status(WebhookEndpointStatus.CIRCUIT_OPEN)
        .circuitOpenedAt(Instant.now().minus(Duration.ofMinutes(10))) // long past cooldown
        .build());
    anEvent(WebhookEventType.EVENT_PUBLISHED);

    fanOutService.fanOutDueEvents();

    var deliveries = webhookDeliveryRepository
        .findByEndpointIdOrderByCreatedAtDesc(coolable.getId(), org.springframework.data.domain.PageRequest.of(0, 10))
        .getContent();
    assertThat(deliveries).hasSize(1);
    assertThat(deliveries.get(0).isProbe()).isTrue();
    assertThat(deliveries.get(0).getState()).isEqualTo(WebhookDeliveryState.PENDING);
  }

  private WebhookEndpoint anEndpoint(WebhookEndpointStatus status, String subscribedType) {
    return webhookEndpointRepository.saveAndFlush(WebhookEndpoint.builder()
        .organizerId(organizerId).url("https://" + UUID.randomUUID() + ".example.com").secret("s")
        .subscribedEventTypes(List.of(subscribedType))
        .status(status).build());
  }

  private WebhookEvent anEvent(WebhookEventType type) {
    return webhookEventRepository.saveAndFlush(WebhookEvent.builder()
        .organizerId(organizerId).type(type)
        .payload(Map.of("eventId", "abc")).occurredAt(Instant.now()).build());
  }
}
