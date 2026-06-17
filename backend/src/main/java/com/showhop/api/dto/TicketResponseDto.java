package com.showhop.api.dto;

import com.showhop.api.entity.enums.TicketStatus;
import java.time.Instant;
import java.util.UUID;

public record TicketResponseDto(
    UUID id,
    UUID ticketTypeId,
    UUID eventId,
    TicketStatus status,
    Instant createdAt) {
}
