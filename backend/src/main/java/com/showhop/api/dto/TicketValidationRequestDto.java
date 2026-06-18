package com.showhop.api.dto;

import com.showhop.api.entity.enums.TicketValidationMethod;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record TicketValidationRequestDto(
    @NotNull UUID ticketId,
    @NotNull TicketValidationMethod method) {
}
