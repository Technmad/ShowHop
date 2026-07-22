package com.showhop.api.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * In-memory (Bucket4j, no Redis) token-bucket limits -- deliberately
 * single-instance, consistent with PRD &sect;4.4's decision to defer Redis
 * until read volume justifies it. Two buckets: the public published-events
 * search endpoint (keyed by client IP, PRD &sect;4.4) and the reservation
 * endpoint (keyed by authenticated user id -- see RateLimitFilter).
 */
@ConfigurationProperties(prefix = "showhop.rate-limit")
public record RateLimitProperties(
    @DefaultValue("true") boolean enabled,
    int searchCapacity,
    Duration searchRefillPeriod,
    int reservationCapacity,
    Duration reservationRefillPeriod) {

  public RateLimitProperties {
    if (searchCapacity <= 0) {
      searchCapacity = 60;
    }
    if (searchRefillPeriod == null) {
      searchRefillPeriod = Duration.ofMinutes(1);
    }
    if (reservationCapacity <= 0) {
      reservationCapacity = 10;
    }
    if (reservationRefillPeriod == null) {
      reservationRefillPeriod = Duration.ofMinutes(1);
    }
  }
}
