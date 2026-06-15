package com.showhop.api.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record TicketTypeResponseDto(
    UUID id,
    UUID eventId,
    String name,
    String description,
    BigDecimal price,
    Integer totalAvailable,
    Instant createdAt,
    Instant updatedAt) {
}
