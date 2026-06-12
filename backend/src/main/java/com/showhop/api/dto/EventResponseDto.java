package com.showhop.api.dto;

import com.showhop.api.entity.enums.EventStatus;
import java.time.Instant;
import java.util.UUID;

public record EventResponseDto(
    UUID id,
    String name,
    String venue,
    Instant startsAt,
    Instant endsAt,
    Instant salesStart,
    Instant salesEnd,
    EventStatus status,
    UUID organizerId,
    Instant createdAt,
    Instant updatedAt) {
}
