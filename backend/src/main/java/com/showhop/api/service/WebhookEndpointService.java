package com.showhop.api.service;

import com.showhop.api.dto.WebhookEndpointPatchDto;
import com.showhop.api.dto.WebhookEndpointRequestDto;
import com.showhop.api.entity.WebhookEndpoint;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface WebhookEndpointService {

  WebhookEndpoint registerEndpoint(UUID organizerId, WebhookEndpointRequestDto request);

  Page<WebhookEndpoint> listEndpoints(UUID organizerId, Pageable pageable);

  WebhookEndpointPatchResult patchEndpoint(
      UUID organizerId, UUID endpointId, WebhookEndpointPatchDto patch);

  /** {@code secretRotated} tells the caller whether to expose the plaintext secret in the response. */
  record WebhookEndpointPatchResult(WebhookEndpoint endpoint, boolean secretRotated) {
  }
}
