package com.showhop.api.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * The only thing that fires {@link ReservationReaper} on a timer -- kept
 * separate so the reaper itself stays a plain, directly callable bean, and
 * this trigger can be switched off with a single property, same pattern as
 * {@code WebhookScheduler}. The test suite disables it
 * ({@code showhop.reservations.scheduling-enabled=false}) so a
 * {@code @SpringBootTest} loading the full context never expires a
 * reservation a test is still mid-assertion on.
 */
@Component
@ConditionalOnProperty(
    prefix = "showhop.reservations", name = "scheduling-enabled",
    havingValue = "true", matchIfMissing = true)
@RequiredArgsConstructor
public class ReservationScheduler {

  private final ReservationReaper reservationReaper;

  @Scheduled(fixedDelayString = "${showhop.reservations.reaper-interval-ms:30000}")
  public void expireDueReservations() {
    reservationReaper.expireDueReservations();
  }
}
