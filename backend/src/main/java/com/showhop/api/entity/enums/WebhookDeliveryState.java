package com.showhop.api.entity.enums;

/**
 * PENDING --claim--> IN_FLIGHT --2xx--> SUCCEEDED
 *                        |
 *                        +-- retryable failure, attempt < maxAttempts --> RETRYING --due--> (re-claimed as IN_FLIGHT)
 *                        |
 *                        +-- failure, attempt == maxAttempts --> DEAD_LETTER
 *
 * SUCCEEDED and DEAD_LETTER are terminal. A replay never resurrects a
 * delivery in place -- it creates a fresh PENDING row for the same
 * (event, endpoint) pair, so this history stays immutable.
 */
public enum WebhookDeliveryState {
  PENDING,
  IN_FLIGHT,
  RETRYING,
  SUCCEEDED,
  DEAD_LETTER
}
