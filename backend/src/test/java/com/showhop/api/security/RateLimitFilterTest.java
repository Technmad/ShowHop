package com.showhop.api.security;

import static org.assertj.core.api.Assertions.assertThat;

import com.showhop.api.config.RateLimitProperties;
import jakarta.servlet.FilterChain;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

/**
 * Exercises the actual 429 behavior directly (no Spring context) --
 * MockMvc-based tests disable rate limiting entirely (see
 * showhop.rate-limit.enabled=false in src/test/resources) since its
 * IP-keyed search bucket would otherwise accumulate across the whole
 * suite, sharing one singleton filter bean and one "127.0.0.1" remote
 * address across every unrelated test.
 */
class RateLimitFilterTest {

  private static final FilterChain NO_OP_CHAIN = (request, response) -> { };

  @Test
  void allowsUpToCapacityThenReturns429WithRetryAfter() throws Exception {
    RateLimitFilter filter = new RateLimitFilter(
        new RateLimitProperties(true, 2, Duration.ofMinutes(1), 10, Duration.ofMinutes(1)));

    for (int i = 0; i < 2; i++) {
      MockHttpServletRequest request = searchRequest();
      MockHttpServletResponse response = new MockHttpServletResponse();
      filter.doFilter(request, response, NO_OP_CHAIN);
      assertThat(response.getStatus()).isEqualTo(200);
    }

    MockHttpServletRequest request = searchRequest();
    MockHttpServletResponse response = new MockHttpServletResponse();
    filter.doFilter(request, response, NO_OP_CHAIN);

    assertThat(response.getStatus()).isEqualTo(429);
    assertThat(response.getHeader("Retry-After")).isNotNull();
    assertThat(response.getContentAsString()).contains("Too many requests");
  }

  @Test
  void requestsOutsideTheTwoMatchedEndpointsAreNeverLimited() throws Exception {
    RateLimitFilter filter = new RateLimitFilter(
        new RateLimitProperties(true, 1, Duration.ofMinutes(1), 1, Duration.ofMinutes(1)));

    for (int i = 0; i < 5; i++) {
      MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/published-events/abc");
      MockHttpServletResponse response = new MockHttpServletResponse();
      filter.doFilter(request, response, NO_OP_CHAIN);
      assertThat(response.getStatus()).isEqualTo(200);
    }
  }

  @Test
  void disabledMeansEveryRequestPassesThrough() throws Exception {
    RateLimitFilter filter = new RateLimitFilter(
        new RateLimitProperties(false, 1, Duration.ofMinutes(1), 1, Duration.ofMinutes(1)));

    for (int i = 0; i < 5; i++) {
      MockHttpServletRequest request = searchRequest();
      MockHttpServletResponse response = new MockHttpServletResponse();
      filter.doFilter(request, response, NO_OP_CHAIN);
      assertThat(response.getStatus()).isEqualTo(200);
    }
  }

  private MockHttpServletRequest searchRequest() {
    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/published-events");
    request.setRemoteAddr("203.0.113.5");
    return request;
  }
}
