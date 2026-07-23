package com.showhop.api.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * {@code key} is {@code null} on every response except the one returned
 * immediately after creation -- see {@code ApiKeyMapper}. The raw key is
 * never stored, so there is no "reveal again later" path.
 */
public record ApiKeyResponseDto(
    UUID id,
    String name,
    String keyPrefix,
    Instant lastUsedAt,
    Instant revokedAt,
    String key,
    Instant createdAt,
    Instant updatedAt) {
}
