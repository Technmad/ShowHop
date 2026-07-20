package com.showhop.api.service.impl;

import com.showhop.api.config.RazorpayProperties;
import com.showhop.api.entity.TicketReservation;
import com.showhop.api.entity.enums.ReservationState;
import com.showhop.api.repository.TicketReservationRepository;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Releases inventory held by abandoned reservations (PRD &sect;4.2): a
 * {@code HELD} reservation past its {@code expiresAt} moves to
 * {@code EXPIRED}, which drops it out of
 * {@code TicketReservationRepository.countActiveHolds} and frees the
 * capacity it was occupying. Claiming is via {@code FOR UPDATE SKIP
 * LOCKED} (see {@code findExpiredHeld}), the same "safely drain a
 * due-work table" primitive {@code WebhookDeliveryWorker} uses -- a
 * concurrent tick, or a fulfillment webhook racing to confirm the same
 * row, each get a disjoint outcome instead of double-processing one.
 */
@Component
@RequiredArgsConstructor
public class ReservationReaper {

  private final TicketReservationRepository ticketReservationRepository;
  private final RazorpayProperties razorpayProperties;
  private final MeterRegistry meterRegistry;

  @Transactional
  public int expireDueReservations() {
    var expired = ticketReservationRepository.findExpiredHeld(razorpayProperties.reaperBatchSize());
    for (TicketReservation reservation : expired) {
      reservation.setState(ReservationState.EXPIRED);
    }
    meterRegistry.counter("showhop.reservations.expired").increment(expired.size());
    return expired.size();
  }
}
