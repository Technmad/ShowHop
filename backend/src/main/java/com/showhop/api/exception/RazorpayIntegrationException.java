package com.showhop.api.exception;

/**
 * Wraps a failure calling out to Razorpay's API (Orders/Refunds) so it maps
 * to a clean, typed HTTP response instead of the framework's default error
 * body. Deliberately a plain {@link RuntimeException}: thrown from inside
 * {@code ReservationServiceImpl.reserve()}'s {@code @Transactional}
 * boundary, it must trigger the default rollback-on-RuntimeException
 * behavior so a failed Order creation never leaves an orphaned
 * {@code TicketReservation} row behind.
 */
public class RazorpayIntegrationException extends RuntimeException {

  public RazorpayIntegrationException(String message, Throwable cause) {
    super(message, cause);
  }
}
