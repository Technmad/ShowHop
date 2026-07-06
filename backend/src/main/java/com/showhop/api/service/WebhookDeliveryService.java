package com.showhop.api.service;

import com.showhop.api.entity.WebhookDelivery;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface WebhookDeliveryService {

  Page<WebhookDelivery> listDeliveriesForEndpoint(UUID organizerId, UUID endpointId, Pageable pageable);

  /**
   * Creates a fresh, non-probe {@code PENDING} delivery for the same
   * (event, endpoint) pair as {@code deliveryId} -- never resurrects the
   * original row, so delivery history stays immutable and auditable.
   */
  WebhookDelivery replayDelivery(UUID organizerId, UUID deliveryId);
}
