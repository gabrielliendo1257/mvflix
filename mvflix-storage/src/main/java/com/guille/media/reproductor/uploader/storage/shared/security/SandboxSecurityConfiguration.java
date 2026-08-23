package com.guille.media.reproductor.uploader.storage.shared.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;

/**
 * Sustituto de {@link SecurityConfiguration} para el perfil {@code sandbox}:
 * sin resource server, todo el API queda accesible para pruebas locales.
 */
@Configuration
@Profile("sandbox")
@EnableWebFluxSecurity
public class SandboxSecurityConfiguration {

  @Bean
  SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
    http.csrf(ServerHttpSecurity.CsrfSpec::disable)
        .authorizeExchange(exchanges -> exchanges.anyExchange().permitAll());

    return http.build();
  }
}