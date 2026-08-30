package com.guille.media.reproductor.uploader.storage.shared.security;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;

class InternalActorUserProviderTest {
  private final InternalActorUserProvider provider =
      new InternalActorUserProvider(new JwtUserProvider());

  @Test
  void internalScopeMaySupplyExplicitActor() {
    var exchange =
        MockServerWebExchange.from(
            org.springframework.mock.http.server.reactive.MockServerHttpRequest.get("/")
                .header("X-Actor-Id", "owner")
                .build());
    var authentication =
        new TestingAuthenticationToken(
            Jwt.withTokenValue("token").header("alg", "none").subject("service").build(),
            null,
            java.util.List.of(new SimpleGrantedAuthority("SCOPE_media-ingestion")));

    var user =
        provider
            .getAuthenticatedUser()
            .contextWrite(ReactiveSecurityContextHolder.withAuthentication(authentication))
            .contextWrite(
                context -> context.put(InternalActorUserProvider.EXCHANGE_CONTEXT_KEY, exchange))
            .block();

    assertThat(user.subject()).isEqualTo("owner");
  }

  @Test
  void userScopeCannotSpoofExplicitActor() {
    var exchange =
        MockServerWebExchange.from(
            org.springframework.mock.http.server.reactive.MockServerHttpRequest.get("/")
                .header("X-Actor-Id", "attacker")
                .build());
    var authentication =
        new TestingAuthenticationToken(
            Jwt.withTokenValue("token").header("alg", "none").subject("real-user").build(), null);
    authentication.setAuthenticated(true);

    var user =
        provider
            .getAuthenticatedUser()
            .contextWrite(ReactiveSecurityContextHolder.withAuthentication(authentication))
            .contextWrite(
                context -> context.put(InternalActorUserProvider.EXCHANGE_CONTEXT_KEY, exchange))
            .block();

    assertThat(user.subject()).isEqualTo("real-user");
  }
}
