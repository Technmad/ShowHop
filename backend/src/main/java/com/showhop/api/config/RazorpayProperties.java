package com.showhop.api.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Tuning knobs for the Razorpay integration (PRD &sect;4.2). {@code keyId}/
 * {@code keySecret} authenticate outbound Orders/Refunds calls;
 * {@code webhookSecret} verifies inbound webhook signatures -- a separate
 * secret from the outbound webhook engine's per-endpoint secrets (&sect;4.1).
 * {@code reservationTtl} is a knob, not a constant, per the PRD's explicit
 * call-out that UPI settlement timing (minutes) versus card timing
 * (seconds) makes this worth tuning rather than hardcoding.
 */
@ConfigurationProperties(prefix = "showhop.razorpay")
public record RazorpayProperties(
    String keyId,
    String keySecret,
    String webhookSecret,
    String baseUrl,
    Duration reservationTtl,
    int reaperBatchSize) {

  public RazorpayProperties {
    if (baseUrl == null || baseUrl.isBlank()) {
      baseUrl = "https://api.razorpay.com/v1";
    }
    if (reservationTtl == null) {
      reservationTtl = Duration.ofMinutes(12);
    }
    if (reaperBatchSize <= 0) {
      reaperBatchSize = 100;
    }
  }
}
