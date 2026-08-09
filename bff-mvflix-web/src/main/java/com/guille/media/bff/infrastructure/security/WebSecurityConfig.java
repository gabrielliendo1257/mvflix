package com.guille.media.bff.infrastructure.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.client.registration.ReactiveClientRegistrationRepository;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.ServerAuthenticationEntryPoint;
import org.springframework.web.server.ServerWebExchange;

import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;

/** Seguridad del BFF: patrón oauth2-client (el navegador nunca ve tokens). */
@Configuration
@EnableWebFluxSecurity
public class WebSecurityConfig {

  @Bean
  SecurityWebFilterChain securityWebFilterChain(
      ServerHttpSecurity http, ReactiveClientRegistrationRepository clientRegistrations) {
    return http
        .csrf(ServerHttpSecurity.CsrfSpec::disable)
        .authorizeExchange(
            exchanges ->
                exchanges
                    .pathMatchers("/web/session", "/login/**", "/oauth2/**", "/error")
                    .permitAll()
                    .pathMatchers("/web/**")
                    .authenticated()
                    .anyExchange()
                    .permitAll())
        .exceptionHandling(handling -> handling.authenticationEntryPoint(new Json401EntryPoint()))
        .oauth2Login(
            oauth2 ->
                oauth2.authorizationRequestResolver(
                    new PkceEnforcingServerOAuth2AuthorizationRequestResolver(
                        clientRegistrations)))
        .build();
  }

  /** 401 en JSON para que el front distinga "sin sesión" y arranque el login. */
  private static final class Json401EntryPoint implements ServerAuthenticationEntryPoint {

    @Override
    public Mono<Void> commence(ServerWebExchange exchange, AuthenticationException ex) {
      exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
      exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
      return exchange
          .getResponse()
          .writeWith(
              Mono.just(
                  exchange
                      .getResponse()
                      .bufferFactory()
                      .wrap("{\"error\":\"unauthorized\"}".getBytes(StandardCharsets.UTF_8))));
    }
  }
}