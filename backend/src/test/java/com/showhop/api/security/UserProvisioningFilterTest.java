package com.showhop.api.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.showhop.api.entity.User;
import com.showhop.api.repository.UserRepository;
import jakarta.servlet.FilterChain;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

@ExtendWith(MockitoExtension.class)
class UserProvisioningFilterTest {

  @Mock
  private UserRepository userRepository;
  @Mock
  private FilterChain filterChain;

  @AfterEach
  void clearSecurityContext() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void createsAUserOnFirstRequestFromASubjectNeverSeenBefore() throws Exception {
    UUID subject = UUID.randomUUID();
    Jwt jwt = jwtFor(subject, "alex", "alex@example.com");
    authenticateAs(jwt);
    when(userRepository.existsById(subject)).thenReturn(false);

    new UserProvisioningFilter(userRepository)
        .doFilterInternal(new MockHttpServletRequest(), new MockHttpServletResponse(), filterChain);

    ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
    verify(userRepository).save(captor.capture());
    assertThat(captor.getValue().getId()).isEqualTo(subject);
    assertThat(captor.getValue().getEmail()).isEqualTo("alex@example.com");
    verify(filterChain).doFilter(any(), any());
  }

  @Test
  void doesNotReprovisionASubjectThatAlreadyExists() throws Exception {
    UUID subject = UUID.randomUUID();
    Jwt jwt = jwtFor(subject, "alex", "alex@example.com");
    authenticateAs(jwt);
    when(userRepository.existsById(subject)).thenReturn(true);

    new UserProvisioningFilter(userRepository)
        .doFilterInternal(new MockHttpServletRequest(), new MockHttpServletResponse(), filterChain);

    verify(userRepository, never()).save(any());
  }

  private void authenticateAs(Jwt jwt) {
    // Mirrors what Spring's resource-server filter actually produces: an
    // authenticated token carrying the roles our JwtAuthenticationConverter
    // extracted (a token built with no authorities defaults to
    // authenticated=false, which would make this test pass for the wrong
    // reason).
    SecurityContextHolder.getContext().setAuthentication(
        new JwtAuthenticationToken(jwt, List.of(new SimpleGrantedAuthority("ROLE_ATTENDEE"))));
  }

  private Jwt jwtFor(UUID subject, String username, String email) {
    return Jwt.withTokenValue("token")
        .header("alg", "none")
        .subject(subject.toString())
        .issuedAt(Instant.now())
        .expiresAt(Instant.now().plusSeconds(60))
        .claim("preferred_username", username)
        .claim("email", email)
        .build();
  }
}
