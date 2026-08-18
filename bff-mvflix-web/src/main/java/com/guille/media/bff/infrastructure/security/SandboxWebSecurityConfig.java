package com.guille.media.bff.infrastructure.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
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
  SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
    return http.csrf(ServerHttpSecurity.CsrfSpec::disable)
        .authorizeExchange(exchanges -> exchanges.anyExchange().permitAll())
        .build();
  }
}