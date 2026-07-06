package com.showhop.api.service.impl;

import com.showhop.api.entity.WebhookDelivery;
import com.showhop.api.exception.WebhookDeliveryNotFoundException;
import com.showhop.api.exception.WebhookEndpointNotFoundException;
import com.showhop.api.repository.WebhookDeliveryRepository;
import com.showhop.api.repository.WebhookEndpointRepository;
import com.showhop.api.service.WebhookDeliveryService;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class WebhookDeliveryServiceImpl implements WebhookDeliveryService {

  private final WebhookEndpointRepository webhookEndpointRepository;
  private final WebhookDeliveryRepository webhookDeliveryRepository;

  @Override
  public Page<WebhookDelivery> listDeliveriesForEndpoint(
      UUID organizerId, UUID endpointId, Pageable pageable) {
    webhookEndpointRepository.findByIdAndOrganizerId(endpointId, organizerId)
        .orElseThrow(() -> new WebhookEndpointNotFoundException(
            "Webhook endpoint with id '%s' was not found".formatted(endpointId)));
    return webhookDeliveryRepository.findByEndpointIdOrderByCreatedAtDesc(endpointId, pageable);
  }

  @Override
  @Transactional
  public WebhookDelivery replayDelivery(UUID organizerId, UUID deliveryId) {
    WebhookDelivery original = webhookDeliveryRepository.findById(deliveryId)
        .filter(delivery -> delivery.getEndpoint().getOrganizerId().equals(organizerId))
        .orElseThrow(() -> new WebhookDeliveryNotFoundException(
            "Webhook delivery with id '%s' was not found".formatted(deliveryId)));

    WebhookDelivery replay = WebhookDelivery.builder()
        .endpoint(original.getEndpoint())
        .event(original.getEvent())
        .maxAttempts(original.getMaxAttempts())
        .probe(false)
        .build();

    return webhookDeliveryRepository.save(replay);
  }
}
