package com.showhop.api.dto;

import com.showhop.api.entity.enums.WebhookDeliveryState;
import com.showhop.api.entity.enums.WebhookEventType;
import java.time.Instant;
import java.util.UUID;

public record WebhookDeliveryResponseDto(
    UUID id,
    WebhookEventType eventType,
    WebhookDeliveryState state,
    int attempt,
    int maxAttempts,
    boolean probe,
    Integer lastResponseCode,
    String lastError,
    Instant nextRetryAt,
    Instant createdAt,
    Instant updatedAt) {
}
