package com.showhop.api.security;

import com.showhop.api.entity.User;
import com.showhop.api.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Creates the local {@link User} row the first time a valid token for a
 * given subject is seen, so there's no separate signup flow -- Keycloak is
 * the source of truth for identity, this just mirrors it locally on first
 * contact.
 */
@Component
@RequiredArgsConstructor
public class UserProvisioningFilter extends OncePerRequestFilter {

  private final UserRepository userRepository;

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {

    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

    if (authentication != null
        && authentication.isAuthenticated()
        && authentication.getPrincipal() instanceof Jwt jwt) {
      provisionIfAbsent(jwt);
    }

    filterChain.doFilter(request, response);
  }

  private void provisionIfAbsent(Jwt jwt) {
    UUID subject = UUID.fromString(jwt.getSubject());

    if (userRepository.existsById(subject)) {
      return;
    }

    User user = User.builder()
        .id(subject)
        .name(jwt.getClaimAsString("preferred_username"))
        .email(jwt.getClaimAsString("email"))
        .build();

    userRepository.save(user);
  }
}
