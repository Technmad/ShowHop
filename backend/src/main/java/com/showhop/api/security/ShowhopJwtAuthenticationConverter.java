package com.showhop.api.security;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

/**
 * Maps Keycloak's {@code realm_access.roles} claim onto Spring
 * {@link GrantedAuthority}s, prefixed {@code ROLE_} so {@code hasRole(...)}
 * matchers work as-is.
 */
@Component
public class ShowhopJwtAuthenticationConverter
    implements Converter<Jwt, AbstractAuthenticationToken> {

  private static final String REALM_ACCESS_CLAIM = "realm_access";
  private static final String ROLES_CLAIM = "roles";
  private static final String ROLE_PREFIX = "ROLE_";

  @Override
  public AbstractAuthenticationToken convert(Jwt jwt) {
    Collection<GrantedAuthority> authorities = extractRealmRoles(jwt).stream()
        .map(role -> new SimpleGrantedAuthority(ROLE_PREFIX + role.toUpperCase()))
        .collect(Collectors.toUnmodifiableSet());

    return new JwtAuthenticationToken(jwt, authorities);
  }

  @SuppressWarnings("unchecked")
  private List<String> extractRealmRoles(Jwt jwt) {
    Map<String, Object> realmAccess = jwt.getClaim(REALM_ACCESS_CLAIM);
    if (realmAccess == null || !(realmAccess.get(ROLES_CLAIM) instanceof List<?> roles)) {
      return List.of();
    }
    return (List<String>) roles;
  }
}
