package com.showhop.api.service;

import com.showhop.api.entity.TicketReservation;
import java.util.UUID;

public interface ReservationService {

  /**
   * Holds inventory and creates the matching Razorpay Order (PRD &sect;4.2).
   * Idempotent on {@code idempotencyKey}: a retried call with the same key
   * returns the original reservation instead of taking a second hold.
   */
  ReservationInitiationResult reserve(
      UUID buyerId, UUID eventId, UUID ticketTypeId, int quantity, String idempotencyKey);

  /** Scoped to the requesting buyer -- one buyer can't poll another's reservation. */
  TicketReservation getForBuyer(UUID buyerId, UUID reservationId);

  /** {@code amountInPaise} is derived at reservation time so callers never recompute it. */
  record ReservationInitiationResult(TicketReservation reservation, long amountInPaise) {
  }
}
