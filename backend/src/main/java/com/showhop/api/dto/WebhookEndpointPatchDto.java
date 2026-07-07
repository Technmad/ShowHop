package com.showhop.api.dto;

import java.util.List;

/**
 * A partial update -- every field is optional and only applied when
 * present. {@code rotateSecret=true} generates a fresh secret; the new
 * plaintext value is the only time it's ever included in a response again
 * after registration.
 */
public record WebhookEndpointPatchDto(
    Boolean enabled,
    List<String> subscribedEventTypes,
    Boolean rotateSecret) {
}
