package com.showhop.api.dto;

import com.showhop.api.entity.enums.ReservationState;
import java.time.Instant;
import java.util.UUID;

/**
 * Everything the frontend needs to open Razorpay's embedded Checkout (PRD
 * &sect;4.2): {@code amount} in paise, {@code razorpayKeyId} is the public
 * key (safe to expose to a client), {@code razorpayOrderId} identifies the
 * Order Checkout attaches the payment to.
 */
public record ReservationInitiationResponseDto(
    UUID id,
    ReservationState state,
    Instant expiresAt,
    String razorpayOrderId,
    String razorpayKeyId,
    long amount) {
}
