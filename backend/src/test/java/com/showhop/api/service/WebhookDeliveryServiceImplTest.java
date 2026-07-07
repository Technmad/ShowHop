package com.showhop.api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.showhop.api.entity.WebhookDelivery;
import com.showhop.api.entity.WebhookEndpoint;
import com.showhop.api.entity.WebhookEvent;
import com.showhop.api.entity.enums.WebhookDeliveryState;
import com.showhop.api.entity.enums.WebhookEndpointStatus;
import com.showhop.api.entity.enums.WebhookEventType;
import com.showhop.api.exception.WebhookDeliveryNotFoundException;
import com.showhop.api.exception.WebhookEndpointNotFoundException;
import com.showhop.api.repository.WebhookDeliveryRepository;
import com.showhop.api.repository.WebhookEndpointRepository;
import com.showhop.api.service.impl.WebhookDeliveryServiceImpl;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class WebhookDeliveryServiceImplTest {

  @Mock
  private WebhookEndpointRepository webhookEndpointRepository;
  @Mock
  private WebhookDeliveryRepository webhookDeliveryRepository;

  @InjectMocks
  private WebhookDeliveryServiceImpl webhookDeliveryService;

  @Test
  void listingDeliveriesRejectsAnEndpointTheOrganizerDoesNotOwn() {
    UUID organizerId = UUID.randomUUID();
    UUID endpointId = UUID.randomUUID();
    when(webhookEndpointRepository.findByIdAndOrganizerId(endpointId, organizerId))
        .thenReturn(Optional.empty());

    assertThatThrownBy(() -> webhookDeliveryService.listDeliveriesForEndpoint(
        organizerId, endpointId, org.springframework.data.domain.PageRequest.of(0, 10)))
        .isInstanceOf(WebhookEndpointNotFoundException.class);
  }

  @Test
  void replayCreatesAFreshPendingNonProbeDeliveryForTheSamePair() {
    UUID organizerId = UUID.randomUUID();
    WebhookEndpoint endpoint = WebhookEndpoint.builder()
        .id(UUID.randomUUID()).organizerId(organizerId).url("https://example.com/hooks")
        .secret("whsec_x").subscribedEventTypes(List.of("event.published"))
        .status(WebhookEndpointStatus.ACTIVE).build();
    WebhookEvent event = WebhookEvent.builder()
        .id(UUID.randomUUID()).organizerId(organizerId).type(WebhookEventType.EVENT_PUBLISHED)
        .payload(Map.of("eventId", "abc")).occurredAt(Instant.now()).build();
    WebhookDelivery original = WebhookDelivery.builder()
        .id(UUID.randomUUID()).endpoint(endpoint).event(event)
        .state(WebhookDeliveryState.DEAD_LETTER).attempt(8).maxAttempts(8).probe(false).build();
    when(webhookDeliveryRepository.findById(original.getId())).thenReturn(Optional.of(original));
    when(webhookDeliveryRepository.save(org.mockito.ArgumentMatchers.any(WebhookDelivery.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    WebhookDelivery replay = webhookDeliveryService.replayDelivery(organizerId, original.getId());

    assertThat(replay).isNotSameAs(original);
    assertThat(replay.getEndpoint()).isSameAs(endpoint);
    assertThat(replay.getEvent()).isSameAs(event);
    assertThat(replay.getState()).isEqualTo(WebhookDeliveryState.PENDING);
    assertThat(replay.getAttempt()).isZero();
    assertThat(replay.isProbe()).isFalse();
  }

  @Test
  void replayRejectsADeliveryBelongingToAnotherOrganizersEndpoint() {
    UUID owner = UUID.randomUUID();
    UUID intruder = UUID.randomUUID();
    WebhookEndpoint endpoint = WebhookEndpoint.builder()
        .id(UUID.randomUUID()).organizerId(owner).url("https://example.com/hooks")
        .secret("whsec_x").subscribedEventTypes(List.of("event.published"))
        .status(WebhookEndpointStatus.ACTIVE).build();
    WebhookEvent event = WebhookEvent.builder()
        .id(UUID.randomUUID()).organizerId(owner).type(WebhookEventType.EVENT_PUBLISHED)
        .payload(Map.of("eventId", "abc")).occurredAt(Instant.now()).build();
    WebhookDelivery original = WebhookDelivery.builder()
        .id(UUID.randomUUID()).endpoint(endpoint).event(event)
        .state(WebhookDeliveryState.DEAD_LETTER).build();
    when(webhookDeliveryRepository.findById(original.getId())).thenReturn(Optional.of(original));

    assertThatThrownBy(() -> webhookDeliveryService.replayDelivery(intruder, original.getId()))
        .isInstanceOf(WebhookDeliveryNotFoundException.class);
  }
}
