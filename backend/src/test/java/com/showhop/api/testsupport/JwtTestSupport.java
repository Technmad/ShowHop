package com.showhop.api.testsupport;

import java.util.UUID;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;

/**
 * Builds a MockMvc {@code jwt()} post-processor with a real UUID subject and
 * the claims {@link com.showhop.api.security.UserProvisioningFilter} needs
 * (a default-built test JWT's "user" subject and missing claims would fail
 * that filter's NOT NULL columns / UUID parsing).
 */
public final class JwtTestSupport {

  private JwtTestSupport() {
  }

  public static RequestPostProcessor authenticatedAs(String role) {
    UUID subject = UUID.randomUUID();
    JwtRequestPostProcessor processor = jwt()
        .jwt(jwt -> jwt.subject(subject.toString())
            .claim("preferred_username", "test-" + role.toLowerCase())
            .claim("email", subject + "@example.com"))
        .authorities(new SimpleGrantedAuthority("ROLE_" + role));
    return processor;
  }

  public static RequestPostProcessor authenticatedAs(String role, UUID subject) {
    JwtRequestPostProcessor processor = jwt()
        .jwt(jwt -> jwt.subject(subject.toString())
            .claim("preferred_username", "test-" + role.toLowerCase())
            .claim("email", subject + "@example.com"))
        .authorities(new SimpleGrantedAuthority("ROLE_" + role));
    return processor;
  }
}
