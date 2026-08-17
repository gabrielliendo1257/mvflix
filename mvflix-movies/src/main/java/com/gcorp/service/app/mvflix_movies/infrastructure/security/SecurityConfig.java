package com.gcorp.service.app.mvflix_movies.infrastructure.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;

@Configuration
@Profile("!sandbox")
@EnableWebFluxSecurity
public class SecurityConfig {

    @Bean
    SecurityWebFilterChain securityWebFilterChain(
        ServerHttpSecurity http, @Value("${security.oauth2.jwk-set-uri}") String jwkSetUri) {
        return http.csrf(ServerHttpSecurity.CsrfSpec::disable)
                .authorizeExchange(
                    exchanges ->
                        exchanges
                            .pathMatchers("/error")
                            .permitAll()
                            .anyExchange()
                            .authenticated())
                .oauth2ResourceServer(
                    resourceServer ->
                        resourceServer.jwt(jwt -> jwt.jwkSetUri(jwkSetUri)))
                .build();
    }
}
