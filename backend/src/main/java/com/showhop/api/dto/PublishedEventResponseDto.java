package com.showhop.api.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * The public-facing shape of a published event -- deliberately narrower
 * than {@link EventResponseDto}. No organizer id, sales window, or audit
 * timestamps: an attendee browsing events has no use for them, and there's
 * no reason to expose internal fields on a public, unauthenticated endpoint.
 */
public record PublishedEventResponseDto(
    UUID id,
    String name,
    String venue,
    Instant startsAt,
    Instant endsAt) {
}
