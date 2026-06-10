package com.showhop.api.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.oauth2.jwt.Jwt;

class ShowhopJwtAuthenticationConverterTest {

  private final ShowhopJwtAuthenticationConverter converter =
      new ShowhopJwtAuthenticationConverter();

  @Test
  void mapsRealmRolesToPrefixedUppercaseAuthorities() {
    Jwt jwt = jwtWithRealmRoles(List.of("organizer", "staff"));

    AbstractAuthenticationToken token = converter.convert(jwt);

    assertThat(token.getAuthorities())
        .extracting(Object::toString)
        .containsExactlyInAnyOrder("ROLE_ORGANIZER", "ROLE_STAFF");
  }

  @Test
  void tolerantOfATokenWithNoRealmAccessClaim() {
    Jwt jwt = Jwt.withTokenValue("token")
        .header("alg", "none")
        .subject("11111111-1111-1111-1111-111111111111")
        .issuedAt(Instant.now())
        .expiresAt(Instant.now().plusSeconds(60))
        .claim("email", "attendee@example.com")
        .build();

    AbstractAuthenticationToken token = converter.convert(jwt);

    assertThat(token.getAuthorities()).isEmpty();
  }

  private Jwt jwtWithRealmRoles(List<String> roles) {
    return Jwt.withTokenValue("token")
        .header("alg", "none")
        .subject("11111111-1111-1111-1111-111111111111")
        .issuedAt(Instant.now())
        .expiresAt(Instant.now().plusSeconds(60))
        .claim("realm_access", Map.of("roles", roles))
        .build();
  }
}
