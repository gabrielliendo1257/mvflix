package com.guille.media.reproductor.users.infra.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity.CsrfSpec;
import org.springframework.security.config.web.server.ServerHttpSecurity.HttpBasicSpec;
import org.springframework.security.web.server.SecurityWebFilterChain;

@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    @Bean
    SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http) {
        http.csrf(CsrfSpec::disable)
                .httpBasic(HttpBasicSpec::disable)
                .oauth2ResourceServer(spec -> spec.jwt(Customizer.withDefaults()))
                .authorizeExchange(
                        authorizeSpec ->
                                authorizeSpec
                                        .pathMatchers("/api/v1/users/me")
                                        .authenticated()
                                        .pathMatchers("/api/v1/users/quota")
                                        .hasAuthority("SCOPE_users.write")
                                        .anyExchange()
                                        .denyAll());

        return http.build();
    }
}