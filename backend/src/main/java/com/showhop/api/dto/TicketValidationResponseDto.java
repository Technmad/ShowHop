package com.showhop.api.dto;

import com.showhop.api.entity.enums.TicketValidationMethod;
import com.showhop.api.entity.enums.TicketValidationStatus;
import java.time.Instant;
import java.util.UUID;

public record TicketValidationResponseDto(
    UUID id,
    UUID ticketId,
    TicketValidationStatus status,
    TicketValidationMethod method,
    Instant validatedAt,
    UUID validatedById) {
}
