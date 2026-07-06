package com.showhop.api.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Tuning knobs for the webhook delivery engine. Defaults are sane for a
 * portfolio-scale deployment; every value here is a knob, not a constant
 * (see the PRD's Risks section on reservation/circuit-breaker tuning).
 */
@ConfigurationProperties(prefix = "showhop.webhooks")
public record WebhookProperties(
    int fanOutBatchSize,
    int claimBatchSize,
    Duration leaseDuration,
    int maxAttempts,
    Duration backoffBase,
    Duration backoffCap,
    int circuitBreakerThreshold,
    Duration circuitBreakerCooldown) {

  public WebhookProperties {
    if (fanOutBatchSize <= 0) {
      fanOutBatchSize = 50;
    }
    if (claimBatchSize <= 0) {
      claimBatchSize = 20;
    }
    if (leaseDuration == null) {
      leaseDuration = Duration.ofMinutes(2);
    }
    if (maxAttempts <= 0) {
      maxAttempts = 8;
    }
    if (backoffBase == null) {
      backoffBase = Duration.ofSeconds(30);
    }
    if (backoffCap == null) {
      backoffCap = Duration.ofHours(1);
    }
    if (circuitBreakerThreshold <= 0) {
      circuitBreakerThreshold = 5;
    }
    if (circuitBreakerCooldown == null) {
      circuitBreakerCooldown = Duration.ofMinutes(5);
    }
  }
}
