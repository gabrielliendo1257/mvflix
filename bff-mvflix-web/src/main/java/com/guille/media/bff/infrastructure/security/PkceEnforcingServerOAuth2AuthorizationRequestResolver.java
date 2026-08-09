package com.guille.media.bff.infrastructure.security;

import org.springframework.security.oauth2.client.registration.ReactiveClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestCustomizers;
import org.springframework.security.oauth2.client.web.server.DefaultServerOAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.client.web.server.ServerOAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.web.server.ServerWebExchange;

import reactor.core.publisher.Mono;

/**
 * Resolver que fuerza PKCE (S256) aunque el cliente sea confidential ({@code client_secret_basic}).
 *
 * <p>El resolver nativo del WebFlux solo aplica PKCE cuando el método de autenticación es
 * {@code none}; el auth server, al tener {@code require-proof-key}, lo exige siempre,
 * así que aquí completamos el {@code code_challenge}/{@code code_verifier} si falta.
 */
public class PkceEnforcingServerOAuth2AuthorizationRequestResolver
    implements ServerOAuth2AuthorizationRequestResolver {

  private static final String CODE_VERIFIER = "code_verifier";

  private final ServerOAuth2AuthorizationRequestResolver delegate;

  public PkceEnforcingServerOAuth2AuthorizationRequestResolver(
      ReactiveClientRegistrationRepository clientRegistrationRepository) {
    this.delegate =
        new DefaultServerOAuth2AuthorizationRequestResolver(clientRegistrationRepository);
  }

  @Override
  public Mono<OAuth2AuthorizationRequest> resolve(ServerWebExchange exchange) {
    return this.delegate.resolve(exchange).map(this::ensurePkce);
  }

  @Override
  public Mono<OAuth2AuthorizationRequest> resolve(
      ServerWebExchange exchange, String clientRegistrationId) {
    return this.delegate.resolve(exchange, clientRegistrationId).map(this::ensurePkce);
  }

  private OAuth2AuthorizationRequest ensurePkce(OAuth2AuthorizationRequest request) {
    if (request.getAttribute(CODE_VERIFIER) != null) {
      return request;
    }
    OAuth2AuthorizationRequest.Builder builder = OAuth2AuthorizationRequest.from(request);
    OAuth2AuthorizationRequestCustomizers.withPkce().accept(builder);
    return builder.build();
  }
}