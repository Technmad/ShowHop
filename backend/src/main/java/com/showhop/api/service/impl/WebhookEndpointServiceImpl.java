package com.showhop.api.service.impl;

import com.showhop.api.dto.WebhookEndpointPatchDto;
import com.showhop.api.dto.WebhookEndpointRequestDto;
import com.showhop.api.entity.WebhookEndpoint;
import com.showhop.api.entity.enums.WebhookEndpointStatus;
import com.showhop.api.exception.WebhookEndpointNotFoundException;
import com.showhop.api.repository.WebhookEndpointRepository;
import com.showhop.api.service.WebhookEndpointService;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class WebhookEndpointServiceImpl implements WebhookEndpointService {

  private final WebhookEndpointRepository webhookEndpointRepository;
  private final SecureRandom secureRandom = new SecureRandom();

  @Override
  @Transactional
  public WebhookEndpoint registerEndpoint(UUID organizerId, WebhookEndpointRequestDto request) {
    WebhookEndpoint endpoint = WebhookEndpoint.builder()
        .organizerId(organizerId)
        .url(request.url())
        .secret(generateSecret())
        .subscribedEventTypes(request.subscribedEventTypes())
        .status(WebhookEndpointStatus.ACTIVE)
        .build();
    return webhookEndpointRepository.save(endpoint);
  }

  @Override
  public Page<WebhookEndpoint> listEndpoints(UUID organizerId, Pageable pageable) {
    return webhookEndpointRepository.findByOrganizerId(organizerId, pageable);
  }

  @Override
  @Transactional
  public WebhookEndpointPatchResult patchEndpoint(
      UUID organizerId, UUID endpointId, WebhookEndpointPatchDto patch) {
    WebhookEndpoint endpoint = webhookEndpointRepository.findByIdAndOrganizerId(endpointId, organizerId)
        .orElseThrow(() -> new WebhookEndpointNotFoundException(
            "Webhook endpoint with id '%s' was not found".formatted(endpointId)));

    if (patch.subscribedEventTypes() != null) {
      endpoint.setSubscribedEventTypes(patch.subscribedEventTypes());
    }

    if (patch.enabled() != null) {
      endpoint.setStatus(patch.enabled() ? WebhookEndpointStatus.ACTIVE : WebhookEndpointStatus.DISABLED);
      if (patch.enabled()) {
        // Re-enabling is a manual override of the circuit breaker too --
        // the organizer is explicitly vouching the endpoint is fixed.
        endpoint.setConsecutiveFailures(0);
        endpoint.setCircuitOpenedAt(null);
      }
    }

    boolean rotated = Boolean.TRUE.equals(patch.rotateSecret());
    if (rotated) {
      endpoint.setSecret(generateSecret());
    }

    return new WebhookEndpointPatchResult(webhookEndpointRepository.save(endpoint), rotated);
  }

  private String generateSecret() {
    byte[] bytes = new byte[32];
    secureRandom.nextBytes(bytes);
    return "whsec_" + Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
  }
}
