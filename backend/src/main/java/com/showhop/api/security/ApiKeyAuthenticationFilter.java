package com.showhop.api.security;

import com.showhop.api.entity.ApiKey;
import com.showhop.api.repository.ApiKeyRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Authenticates the {@code X-Api-Key} header as an alternate credential to
 * the Keycloak JWT, scoped to the webhook-management API only (PRD
 * &sect;4.4: "programmatic access ... for the webhook-management API
 * itself"). Registered before {@code BearerTokenAuthenticationFilter} (see
 * SecurityConfig) so it only acts when the header is present; a request
 * carrying a real JWT and no API key flows through untouched.
 *
 * <p>Sets a <em>synthetic</em> {@link Jwt} as the authentication principal
 * -- not the raw organizer id -- purely so every existing controller
 * (written against {@code @AuthenticationPrincipal Jwt jwt} +
 * {@code JwtUtil.parseUserId(jwt)}) keeps working unmodified regardless of
 * which credential authenticated the request. It is never validated as a
 * real token; it only ever exists in memory for the lifetime of this
 * request.
 */
@RequiredArgsConstructor
public class ApiKeyAuthenticationFilter extends OncePerRequestFilter {

  private static final String HEADER = "X-Api-Key";
  private static final List<GrantedAuthority> ORGANIZER_AUTHORITY =
      List.of(new SimpleGrantedAuthority("ROLE_ORGANIZER"));

  private final ApiKeyRepository apiKeyRepository;

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    String rawKey = request.getHeader(HEADER);
    if (rawKey == null || rawKey.isBlank()) {
      filterChain.doFilter(request, response);
      return;
    }

    apiKeyRepository.findByKeyPrefixAndRevokedAtIsNull(ApiKeyHasher.prefixOf(rawKey))
        .filter(apiKey -> apiKey.getHashedKey().equals(ApiKeyHasher.hash(rawKey)))
        .ifPresent(this::authenticate);

    filterChain.doFilter(request, response);
  }

  private void authenticate(ApiKey apiKey) {
    apiKey.setLastUsedAt(Instant.now());
    apiKeyRepository.save(apiKey);

    Instant now = Instant.now();
    Jwt syntheticJwt = Jwt.withTokenValue("api-key:" + apiKey.getId())
        .header("alg", "none")
        .subject(apiKey.getOrganizerId().toString())
        .claim("preferred_username", "api-key:" + apiKey.getName())
        .issuedAt(now)
        .expiresAt(now.plusSeconds(60))
        .build();

    SecurityContextHolder.getContext().setAuthentication(
        new UsernamePasswordAuthenticationToken(syntheticJwt, null, ORGANIZER_AUTHORITY));
  }
}
