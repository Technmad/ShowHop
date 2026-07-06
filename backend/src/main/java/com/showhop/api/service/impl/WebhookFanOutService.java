package com.showhop.api.service.impl;

import com.showhop.api.config.WebhookProperties;
import com.showhop.api.entity.WebhookDelivery;
import com.showhop.api.entity.WebhookEndpoint;
import com.showhop.api.entity.WebhookEvent;
import com.showhop.api.entity.enums.WebhookDeliveryState;
import com.showhop.api.entity.enums.WebhookEndpointStatus;
import com.showhop.api.repository.WebhookDeliveryRepository;
import com.showhop.api.repository.WebhookEndpointRepository;
import com.showhop.api.repository.WebhookEventRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Fans committed {@link WebhookEvent}s out to subscribed endpoints by
 * creating {@link WebhookDelivery} rows -- it never sends HTTP itself, that
 * is {@code WebhookDeliveryWorker}'s job. Invoked on a schedule by
 * {@code WebhookScheduler} (kept separate so this bean stays trivially
 * testable/callable on its own); the {@code PESSIMISTIC_WRITE} lock on
 * {@code findUnfannedOut} means two instances of this relay running
 * concurrently can't both fan out the same event.
 */
@Service
@RequiredArgsConstructor
public class WebhookFanOutService {

  private static final List<WebhookDeliveryState> TERMINAL_STATES =
      List.of(WebhookDeliveryState.SUCCEEDED, WebhookDeliveryState.DEAD_LETTER);

  private final WebhookEventRepository webhookEventRepository;
  private final WebhookEndpointRepository webhookEndpointRepository;
  private final WebhookDeliveryRepository webhookDeliveryRepository;
  private final WebhookProperties properties;

  @Transactional
  public int fanOutDueEvents() {
    List<WebhookEvent> events =
        webhookEventRepository.findUnfannedOut(PageRequest.of(0, properties.fanOutBatchSize()));

    int created = 0;
    for (WebhookEvent event : events) {
      for (Candidate candidate : candidatesFor(event)) {
        webhookDeliveryRepository.save(WebhookDelivery.builder()
            .endpoint(candidate.endpoint())
            .event(event)
            .probe(candidate.probe())
            .maxAttempts(properties.maxAttempts())
            .build());
        created++;
      }
      event.setFannedOutAt(Instant.now());
    }
    return created;
  }

  private List<Candidate> candidatesFor(WebhookEvent event) {
    List<Candidate> candidates = new ArrayList<>();
    String wireType = event.getType().wireValue();

    for (WebhookEndpoint endpoint : webhookEndpointRepository
        .findByOrganizerIdAndStatus(event.getOrganizerId(), WebhookEndpointStatus.ACTIVE)) {
      if (subscribed(endpoint, wireType)) {
        candidates.add(new Candidate(endpoint, false));
      }
    }

    for (WebhookEndpoint endpoint : webhookEndpointRepository
        .findByOrganizerIdAndStatus(event.getOrganizerId(), WebhookEndpointStatus.CIRCUIT_OPEN)) {
      if (subscribed(endpoint, wireType) && cooldownElapsed(endpoint) && !hasInFlightProbe(endpoint)) {
        candidates.add(new Candidate(endpoint, true));
      }
    }

    return candidates;
  }

  private boolean subscribed(WebhookEndpoint endpoint, String wireType) {
    return endpoint.getSubscribedEventTypes() != null
        && endpoint.getSubscribedEventTypes().contains(wireType);
  }

  private boolean cooldownElapsed(WebhookEndpoint endpoint) {
    return endpoint.getCircuitOpenedAt() != null
        && Instant.now().isAfter(endpoint.getCircuitOpenedAt().plus(properties.circuitBreakerCooldown()));
  }

  private boolean hasInFlightProbe(WebhookEndpoint endpoint) {
    return webhookDeliveryRepository
        .existsByEndpointIdAndProbeTrueAndStateNotIn(endpoint.getId(), TERMINAL_STATES);
  }

  private record Candidate(WebhookEndpoint endpoint, boolean probe) {
  }
}
