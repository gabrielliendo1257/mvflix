package com.guille.media.bff.infrastructure.http;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.ReactiveOAuth2AuthorizedClientProvider;
import org.springframework.security.oauth2.client.web.server.ServerOAuth2AuthorizedClientRepository;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import org.springframework.web.server.ServerWebExchange;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

class BffWebClientConfigurationTest {

  private final ServerOAuth2AuthorizedClientRepository repository = mock(ServerOAuth2AuthorizedClientRepository.class);
  private final ReactiveOAuth2AuthorizedClientProvider refreshProvider = mock(ReactiveOAuth2AuthorizedClientProvider.class);
  private final ExchangeFunction next = mock(ExchangeFunction.class);
  private final Authentication auth = new UsernamePasswordAuthenticationToken("user", null, List.of());
  private final ServerWebExchange exchange = mock(ServerWebExchange.class);

  private final BffWebClientConfiguration configuration = new BffWebClientConfiguration();

  private ClientRequest request() {
    return ClientRequest.create(HttpMethod.GET, URI.create("http://backend/resource"))
        .attribute(ServerWebExchange.class.getName(), this.exchange)
        .build();
  }

  private OAuth2AuthorizedClient authorizedClient(Instant expiresAt) {
    OAuth2AuthorizedClient client = mock(OAuth2AuthorizedClient.class);
    when(client.getAccessToken())
        .thenReturn(new org.springframework.security.oauth2.core.OAuth2AccessToken(
            org.springframework.security.oauth2.core.OAuth2AccessToken.TokenType.BEARER,
            "access-token", expiresAt.minusSeconds(120), expiresAt));
    return client;
  }

  @Test
  void refreshesExpiredSessionTokenAndSavesItBack() {
    OAuth2AuthorizedClient expired = this.authorizedClient(Instant.now().minusSeconds(30));
    OAuth2AuthorizedClient refreshed = this.authorizedClient(Instant.now().plusSeconds(300));
    when(this.repository.loadAuthorizedClient(any(), any(), any())).thenReturn(Mono.just(expired));
    when(this.repository.saveAuthorizedClient(any(), any(), any())).thenReturn(Mono.empty());
    when(this.refreshProvider.authorize(any()))
        .thenReturn(Mono.just(refreshed));
    when(this.next.exchange(any())).thenReturn(Mono.just(ClientResponse.create(HttpStatus.OK).build()));

    var filter = this.configuration.oauth2AccessTokenRefreshFilter(
        this.repository, this.refreshProvider, "movie-app");

    StepVerifier.create(filter.filter(this.request(), this.next)
            .contextWrite(ReactiveSecurityContextHolder.withAuthentication(this.auth)))
        .assertNext(response -> {
        })
        .verifyComplete();

    verify(this.repository).saveAuthorizedClient(refreshed, this.auth, this.exchange);
    verify(this.next).exchange(any());
  }

  @Test
  void doesNotRefreshWhenTokenIsStillValid() {
    OAuth2AuthorizedClient valid = this.authorizedClient(Instant.now().plusSeconds(300));
    when(this.repository.loadAuthorizedClient(any(), any(), any())).thenReturn(Mono.just(valid));
    when(this.next.exchange(any())).thenReturn(Mono.just(ClientResponse.create(HttpStatus.OK).build()));

    var filter = this.configuration.oauth2AccessTokenRefreshFilter(
        this.repository, this.refreshProvider, "movie-app");

    StepVerifier.create(filter.filter(this.request(), this.next)
            .contextWrite(ReactiveSecurityContextHolder.withAuthentication(this.auth)))
        .assertNext(response -> {
        })
        .verifyComplete();

    verify(this.repository, never()).saveAuthorizedClient(any(), any(), any());
    verify(this.refreshProvider, never()).authorize(any());
    verify(this.next).exchange(any());
  }

  @Test
  void withoutExchangeAttributePassesThrough() {
    when(this.next.exchange(any())).thenReturn(Mono.just(ClientResponse.create(HttpStatus.OK).build()));
    var filter = this.configuration.oauth2AccessTokenRefreshFilter(
        this.repository, this.refreshProvider, "movie-app");

    ClientRequest bare = ClientRequest.create(HttpMethod.GET, URI.create("http://backend/resource")).build();

    StepVerifier.create(filter.filter(bare, this.next)
            .contextWrite(ReactiveSecurityContextHolder.withAuthentication(this.auth)))
        .assertNext(response -> {
        })
        .verifyComplete();

    verify(this.repository, never()).loadAuthorizedClient(any(), any(), any());
  }
}