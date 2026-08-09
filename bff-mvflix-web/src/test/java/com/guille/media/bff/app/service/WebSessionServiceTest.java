package com.guille.media.bff.app.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.guille.media.bff.app.dto.UserProfile;
import com.guille.media.bff.app.ports.UsersWebPort;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

class WebSessionServiceTest {

  private final UsersWebPort usersWebClient = mock(UsersWebPort.class);
  private WebSessionService service;

  @BeforeEach
  void setUp() {
    this.service = new WebSessionService(this.usersWebClient);
  }

  @Test
  void subjectIsTakenFromAuthenticatedUserProfiles() {
    when(usersWebClient.me("Bearer x")).thenReturn(Mono.just(new UserProfile("1", "pepe", "pepe@mvflix.dev", "PRO", true)));

    StepVerifier.create(service.subject("Bearer x"))
        .expectNext("pepe")
        .verifyComplete();
  }

  @Test
  void rejectsMissingToken() {
    StepVerifier.create(service.subject(null))
        .expectError(ResponseStatusException.class)
        .verify();
    StepVerifier.create(service.subject("  "))
        .expectError(ResponseStatusException.class)
        .verify();
  }

  @Test
  void rejectsInvalidTokenWhenUsersServiceHasNoProfile() {
    when(usersWebClient.me("Bearer malo")).thenReturn(Mono.empty());

    StepVerifier.create(service.subject("Bearer malo"))
        .expectError(ResponseStatusException.class)
        .verify();
  }
}