package com.showhop.api.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.showhop.api.config.WebhookProperties;
import com.showhop.api.entity.WebhookDelivery;
import com.showhop.api.entity.WebhookEndpoint;
import com.showhop.api.entity.enums.WebhookDeliveryState;
import com.showhop.api.entity.enums.WebhookEndpointStatus;
import com.showhop.api.entity.enums.WebhookEventType;
import com.showhop.api.repository.WebhookDeliveryRepository;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

/**
 * Claims due deliveries via {@code FOR UPDATE SKIP LOCKED} (a competing-
 * consumers model -- safe with any number of worker instances), sends each
 * one with no DB transaction held open during the HTTP call, then records
 * the outcome in a fresh, short transaction. See
 * {@code WebhookDeliveryRepository.findClaimable} for why the claim and the
 * send are deliberately two separate transactions. Invoked on a schedule by
 * {@code WebhookScheduler}, kept separate so this bean stays callable
 * directly (e.g. from tests that don't want a background thread involved).
 */
@Component
@RequiredArgsConstructor
public class WebhookDeliveryWorker {

  private final WebhookDeliveryRepository webhookDeliveryRepository;
  private final RestClient restClient;
  private final ObjectMapper objectMapper;
  private final WebhookProperties properties;
  private final WebhookSigner webhookSigner;
  private final String workerId = "worker-" + UUID.randomUUID();

  public void pollAndDeliver() {
    for (DeliveryTask task : claimBatch()) {
      send(task);
    }
  }

  @Transactional
  public List<DeliveryTask> claimBatch() {
    List<WebhookDelivery> claimed = webhookDeliveryRepository.findClaimable(properties.claimBatchSize());
    Instant lease = Instant.now().plus(properties.leaseDuration());

    return claimed.stream().map(delivery -> {
      delivery.setState(WebhookDeliveryState.IN_FLIGHT);
      delivery.setLockedBy(workerId);
      delivery.setLockedUntil(lease);
      delivery.setAttempt(delivery.getAttempt() + 1);
      return new DeliveryTask(
          delivery.getId(),
          delivery.getEndpoint().getUrl(),
          delivery.getEndpoint().getSecret(),
          delivery.getEvent().getType(),
          delivery.getEvent().getPayload(),
          delivery.getAttempt());
    }).toList();
  }

  private void send(DeliveryTask task) {
    String body;
    try {
      body = objectMapper.writeValueAsString(Map.of(
          "type", task.type().wireValue(),
          "data", task.payload()));
    } catch (JsonProcessingException e) {
      recordOutcome(task.deliveryId(), false, null, "Failed to serialize payload: " + e.getMessage());
      return;
    }

    String timestamp = String.valueOf(Instant.now().getEpochSecond());
    String signature = webhookSigner.sign(task.secret(), timestamp, body);

    try {
      restClient.post()
          .uri(task.url())
          .header("Webhook-Signature", signature)
          .header("Webhook-Timestamp", timestamp)
          .header("Idempotency-Key", task.deliveryId().toString())
          .contentType(MediaType.APPLICATION_JSON)
          .body(body)
          .retrieve()
          .toBodilessEntity();
      recordOutcome(task.deliveryId(), true, 200, null);
    } catch (RestClientResponseException e) {
      recordOutcome(task.deliveryId(), false, e.getStatusCode().value(), e.getMessage());
    } catch (RestClientException e) {
      recordOutcome(task.deliveryId(), false, null, e.getMessage());
    }
  }

  @Transactional
  public void recordOutcome(UUID deliveryId, boolean success, Integer responseCode, String error) {
    WebhookDelivery delivery = webhookDeliveryRepository.findById(deliveryId).orElseThrow();
    WebhookEndpoint endpoint = delivery.getEndpoint();

    delivery.setLastResponseCode(responseCode);
    delivery.setLastError(error);

    if (success) {
      delivery.setState(WebhookDeliveryState.SUCCEEDED);
      endpoint.setConsecutiveFailures(0);
      if (delivery.isProbe()) {
        endpoint.setStatus(WebhookEndpointStatus.ACTIVE);
        endpoint.setCircuitOpenedAt(null);
      }
      return;
    }

    if (delivery.isProbe()) {
      // The half-open probe failed: stay open and restart the cooldown.
      // The probe itself is terminal -- the relay creates a fresh one
      // after the next cooldown elapses, rather than retrying this row.
      endpoint.setCircuitOpenedAt(Instant.now());
      delivery.setState(WebhookDeliveryState.DEAD_LETTER);
      return;
    }

    endpoint.setConsecutiveFailures(endpoint.getConsecutiveFailures() + 1);
    if (endpoint.getStatus() == WebhookEndpointStatus.ACTIVE
        && endpoint.getConsecutiveFailures() >= properties.circuitBreakerThreshold()) {
      endpoint.setStatus(WebhookEndpointStatus.CIRCUIT_OPEN);
      endpoint.setCircuitOpenedAt(Instant.now());
    }

    if (delivery.getAttempt() >= delivery.getMaxAttempts()) {
      delivery.setState(WebhookDeliveryState.DEAD_LETTER);
    } else {
      delivery.setState(WebhookDeliveryState.RETRYING);
      delivery.setNextRetryAt(nextRetryAt(delivery.getAttempt()));
    }
  }

  private Instant nextRetryAt(int attempt) {
    long baseMs = properties.backoffBase().toMillis();
    long capMs = properties.backoffCap().toMillis();
    long delay = Math.min(baseMs * (1L << Math.min(attempt, 20)), capMs);
    long jittered = delay / 2 + ThreadLocalRandom.current().nextLong(Math.max(delay / 2, 1));
    return Instant.now().plusMillis(jittered);
  }

  /** Everything the HTTP send needs, captured while the claiming transaction still has the entities attached. */
  private record DeliveryTask(
      UUID deliveryId, String url, String secret, WebhookEventType type,
      Map<String, Object> payload, int attempt) {
  }
}
