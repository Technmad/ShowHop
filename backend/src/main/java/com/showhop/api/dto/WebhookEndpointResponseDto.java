package com.showhop.api.dto;

import com.showhop.api.entity.enums.WebhookEndpointStatus;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * {@code secret} is {@code null} on every response except immediately after
 * registration or a secret rotation -- see {@code WebhookEndpointMapper}.
 */
public record WebhookEndpointResponseDto(
    UUID id,
    String url,
    List<String> subscribedEventTypes,
    WebhookEndpointStatus status,
    int consecutiveFailures,
    Instant circuitOpenedAt,
    String secret,
    Instant createdAt,
    Instant updatedAt) {
}
