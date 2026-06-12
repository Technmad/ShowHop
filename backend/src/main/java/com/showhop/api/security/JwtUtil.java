package com.showhop.api.security;

import java.util.UUID;
import org.springframework.security.oauth2.jwt.Jwt;

public final class JwtUtil {

  private JwtUtil() {
  }

  /** The Keycloak subject claim, which doubles as the local {@code User} id. */
  public static UUID parseUserId(Jwt jwt) {
    return UUID.fromString(jwt.getSubject());
  }
}
