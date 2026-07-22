package com.showhop.api.security;

import com.showhop.api.config.RateLimitProperties;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import io.github.bucket4j.Refill;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * In-memory (Bucket4j) token buckets for the two endpoints PRD &sect;4.4
 * names: the anonymous published-events search (keyed by client IP -- no
 * JWT to key on) and the authenticated reservation POST (keyed by the
 * buyer's user id, so one buyer can't be starved by another sharing a NAT).
 * Registered after {@link UserProvisioningFilter} (see SecurityConfig) so
 * the reservation branch runs with the JWT already authenticated and the
 * local User already provisioned.
 */
@RequiredArgsConstructor
public class RateLimitFilter extends OncePerRequestFilter {

  private static final String RESERVATION_PATTERN =
      "/api/v1/published-events/*/ticket-types/*/reservations";

  private final RateLimitProperties properties;
  private final AntPathMatcher pathMatcher = new AntPathMatcher();
  private final ConcurrentHashMap<String, Bucket> searchBuckets = new ConcurrentHashMap<>();
  private final ConcurrentHashMap<String, Bucket> reservationBuckets = new ConcurrentHashMap<>();

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    if (!properties.enabled()) {
      filterChain.doFilter(request, response);
      return;
    }

    Bucket bucket = resolveBucket(request);
    if (bucket == null) {
      filterChain.doFilter(request, response);
      return;
    }

    ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);
    if (probe.isConsumed()) {
      response.setHeader("X-RateLimit-Remaining", String.valueOf(probe.getRemainingTokens()));
      filterChain.doFilter(request, response);
      return;
    }

    long retryAfterSeconds = Math.max(1, probe.getNanosToWaitForRefill() / 1_000_000_000);
    response.setStatus(429);
    response.setHeader("Retry-After", String.valueOf(retryAfterSeconds));
    response.setContentType("application/json");
    response.getWriter().write(
        "{\"status\":429,\"message\":\"Too many requests, retry after %d seconds\"}"
            .formatted(retryAfterSeconds));
  }

  private Bucket resolveBucket(HttpServletRequest request) {
    if ("GET".equalsIgnoreCase(request.getMethod())
        && "/api/v1/published-events".equals(request.getRequestURI())) {
      return searchBuckets.computeIfAbsent(clientIpKey(request), key ->
          newBucket(properties.searchCapacity(), properties.searchRefillPeriod()));
    }
    if ("POST".equalsIgnoreCase(request.getMethod())
        && pathMatcher.match(RESERVATION_PATTERN, request.getRequestURI())) {
      return reservationBuckets.computeIfAbsent(reservationKey(request), key ->
          newBucket(properties.reservationCapacity(), properties.reservationRefillPeriod()));
    }
    return null;
  }

  private String clientIpKey(HttpServletRequest request) {
    return "ip:" + request.getRemoteAddr();
  }

  private String reservationKey(HttpServletRequest request) {
    var authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication != null && authentication.getPrincipal() instanceof Jwt jwt) {
      return "user:" + JwtUtil.parseUserId(jwt);
    }
    return clientIpKey(request);
  }

  private Bucket newBucket(int capacity, Duration refillPeriod) {
    Bandwidth limit = Bandwidth.classic(capacity, Refill.greedy(capacity, refillPeriod));
    return Bucket.builder().addLimit(limit).build();
  }
}
