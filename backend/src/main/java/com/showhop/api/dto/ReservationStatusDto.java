package com.showhop.api.dto;

import com.showhop.api.entity.enums.ReservationState;
import java.time.Instant;
import java.util.UUID;

/** What a buyer polls while Checkout is open (PRD &sect;4.2): "processing..." until CONFIRMED. */
public record ReservationStatusDto(
    UUID id,
    ReservationState state,
    Instant expiresAt) {
}
