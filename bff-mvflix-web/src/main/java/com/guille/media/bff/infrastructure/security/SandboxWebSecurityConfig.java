package com.guille.media.bff.infrastructure.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UserDetailsRepositoryReactiveAuthenticationManager;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.userdetails.MapReactiveUserDetailsService;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.web.server.SecurityWebFilterChain;

/**
 * Sustituto de {@link WebSecurityConfig} para el perfil {@code sandbox}:
 * sin oauth2-client ni resource server, todo el API queda accesible para
 * probar desde Postman sin pasar por el IdP.
 */
@Configuration
@Profile("sandbox")
@EnableWebFluxSecurity
public class SandboxWebSecurityConfig {

  @Bean
  SecurityWebFilterChain securityWebFilterChain(
      ServerHttpSecurity http,
      @Value("${ACTUATOR_METRICS_USER:metrics}") String username,
      @Value("${ACTUATOR_METRICS_PASSWORD:change-me}") String password) {
    return http.csrf(ServerHttpSecurity.CsrfSpec::disable)
        .authenticationManager(new UserDetailsRepositoryReactiveAuthenticationManager(
            new MapReactiveUserDetailsService(User.withUsername(username)
                .password("{noop}" + password).roles("METRICS").build())))
        .httpBasic(org.springframework.security.config.Customizer.withDefaults())
        .authorizeExchange(exchanges -> exchanges.pathMatchers("/actuator/**").hasRole("METRICS")
            .anyExchange().permitAll())
        .build();
  }
}
