package com.showhop.api.config;

import com.showhop.api.security.ShowhopJwtAuthenticationConverter;
import com.showhop.api.security.UserProvisioningFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

  @Bean
  public SecurityFilterChain filterChain(
      HttpSecurity http,
      ShowhopJwtAuthenticationConverter jwtAuthenticationConverter,
      UserProvisioningFilter userProvisioningFilter) throws Exception {
    http
        .authorizeHttpRequests(authorize -> authorize
            .requestMatchers(HttpMethod.GET, "/api/v1/published-events/**").permitAll()
            // "/**", not just the bare path -- so sub-resources like
            // /api/v1/events/{id} are covered by the same matcher instead
            // of silently falling through to the generic authenticated()
            // rule below.
            .requestMatchers("/api/v1/events/**").hasRole("ORGANIZER")
            .requestMatchers("/api/v1/webhook-endpoints/**").hasRole("ORGANIZER")
            .requestMatchers("/api/v1/webhook-deliveries/**").hasRole("ORGANIZER")
            .requestMatchers("/api/v1/ticket-validations/**").hasRole("STAFF")
            .anyRequest().authenticated())
        .csrf(csrf -> csrf.disable())
        .sessionManagement(session ->
            session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .oauth2ResourceServer(oauth2 -> oauth2
            .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter)))
        .addFilterAfter(userProvisioningFilter, BearerTokenAuthenticationFilter.class);

    return http.build();
  }
}
