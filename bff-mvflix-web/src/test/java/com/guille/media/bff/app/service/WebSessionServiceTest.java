package com.guille.media.bff.app.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;

import reactor.test.StepVerifier;

class WebSessionServiceTest {

  private final WebSessionService service = new WebSessionService();

  @Test
  void subjectIsTakenFromAuthenticatedSession() {
    var auth =
        new UsernamePasswordAuthenticationToken("pepe", null, java.util.List.of());

    StepVerifier.create(
            service.currentSubject().contextWrite(ReactiveSecurityContextHolder.withAuthentication(auth)))
        .assertNext(subject -> assertThat(subject).isEqualTo("pepe"))
        .verifyComplete();
  }

  @Test
  void emptyWhenThereIsNoSession() {
    StepVerifier.create(service.currentSubject()).verifyComplete();
  }

  @Test
  void emptyWhenPrincipalIsAnonymous() {
    var anon =
        new org.springframework.security.authentication.AnonymousAuthenticationToken(
            "anonymous",
            "anonymousUser",
            java.util.List.of(
                new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_ANONYMOUS")));

    StepVerifier.create(
            service.currentSubject().contextWrite(ReactiveSecurityContextHolder.withAuthentication(anon)))
        .verifyComplete();
  }
}