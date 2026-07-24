package com.showhop.api.config;

import org.springframework.boot.actuate.autoconfigure.security.servlet.EndpointRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Actuator lives on its own port ({@code management.server.port=8081}, see
 * application.properties) specifically so ops/scraping traffic never has
 * to present a Keycloak JWT -- but Spring Boot still auto-secures that
 * separate context by default once {@code spring-boot-starter-security}
 * is on the classpath, requiring authentication with no way to satisfy
 * it (every actuator request 401s). This explicit, higher-precedence
 * chain replaces that default with a permitAll scoped to actuator
 * endpoints only, trusting network topology (an internal-only port,
 * never exposed the way the main API port is) rather than app-level auth
 * to keep it private.
 */
@Configuration
public class ActuatorSecurityConfig {

  @Bean
  @Order(0)
  public SecurityFilterChain actuatorSecurityFilterChain(HttpSecurity http) throws Exception {
    http.securityMatcher(EndpointRequest.toAnyEndpoint())
        .authorizeHttpRequests(authorize -> authorize.anyRequest().permitAll())
        .csrf(csrf -> csrf.disable());
    return http.build();
  }
}
