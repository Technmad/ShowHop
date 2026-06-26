package com.showhop.api.entity.enums;

/**
 * Domain events the webhook engine can publish. The enum name is the JPA
 * storage form; {@link #wireValue()} is the dot-notation form sent over the
 * wire and matched against {@code WebhookEndpoint.subscribedEventTypes}, the
 * same convention Stripe/GitHub/Svix use.
 */
public enum WebhookEventType {
  EVENT_PUBLISHED("event.published"),
  TICKET_PURCHASED("ticket.purchased"),
  TICKET_VALIDATED("ticket.validated");

  private final String wireValue;

  WebhookEventType(String wireValue) {
    this.wireValue = wireValue;
  }

  public String wireValue() {
    return wireValue;
  }
}
