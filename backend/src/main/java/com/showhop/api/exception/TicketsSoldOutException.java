package com.showhop.api.exception;

/** Thrown when a purchase would push sold tickets past a type's capacity. */
public class TicketsSoldOutException extends RuntimeException {

  public TicketsSoldOutException(String message) {
    super(message);
  }
}
