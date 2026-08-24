package com.guille.media.bff.experience.shell.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.guille.media.bff.app.dto.UserProfile;
import com.guille.media.bff.app.ports.UsersWebPort;
import com.guille.media.bff.experience.shell.application.port.CurrentPrincipal;
import com.guille.media.bff.experience.shell.application.port.CurrentPrincipal.PrincipalIdentity;

import org.junit.jupiter.api.Test;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.Set;

class GetShellContextTest {

  private final CurrentPrincipal principal = mock(CurrentPrincipal.class);
  private final UsersWebPort users = mock(UsersWebPort.class);
  private final ActivitySummary activity = mock(ActivitySummary.class);

  private final GetShellContext useCase =
      new GetShellContext(this.principal, this.users, this.activity);

  private void principalIs(String subject, Set<String> authorities) {
    when(this.principal.current())
        .thenReturn(Mono.just(new PrincipalIdentity(subject, authorities)));
  }

  private void anonymousPrincipal() {
    when(this.principal.current()).thenReturn(Mono.empty());
  }

  private UserProfile profile(boolean blocked) {
    return new UserProfile("5bb", "Admin", "admin@mvflix.local", "ENTERPRISE",
        true, 0, blocked);
  }

  private void activityCounts(long running, long failed) {
    when(this.activity.summaryFor(org.mockito.ArgumentMatchers.anyString()))
        .thenReturn(Mono.just(new ShellActivity(running, failed)));
  }

  @Test
  void anonymousGetsEmptyUserNullActivityAndNoCapabilities() {
    this.anonymousPrincipal();

    StepVerifier.create(this.useCase.execute())
        .assertNext(context -> {
          assertThat(context.authenticated()).isFalse();
          assertThat(context.user()).isNull();
          assertThat(context.capabilities()).isEqualTo(ShellCapabilities.none());
          assertThat(context.activity()).isNull();
        })
        .verifyComplete();

    org.mockito.Mockito.verifyNoInteractions(this.users, this.activity);
  }

  @Test
  void authenticatedNormalUserSeesOwnActivityAndCanManage() {
    this.principalIs("pepe", Set.of());
    when(this.users.me()).thenReturn(Mono.just(profile(false)));
    this.activityCounts(1, 0);

    StepVerifier.create(this.useCase.execute())
        .assertNext(context -> {
          assertThat(context.authenticated()).isTrue();
          assertThat(context.user().id()).isEqualTo("5bb");
          assertThat(context.user().username()).isEqualTo("Admin");
          assertThat(context.capabilities().canAddMedia()).isTrue();
          assertThat(context.capabilities().canManageLibraries()).isTrue();
          assertThat(context.capabilities().canAccessAdmin()).isFalse();
          assertThat(context.activity().running()).isEqualTo(1);
        })
        .verifyComplete();
  }

  @Test
  void adminRoleGrantsAdminCapability() {
    this.principalIs("admin", Set.of("ROLE_ADMIN"));
    when(this.users.me()).thenReturn(Mono.just(profile(false)));
    this.activityCounts(0, 0);

    StepVerifier.create(this.useCase.execute())
        .assertNext(context ->
            assertThat(context.capabilities().canAccessAdmin()).isTrue())
        .verifyComplete();
  }

  @Test
  void blockedUserLosesProductCapabilitiesButKeepsIdentity() {
    this.principalIs("pepe", Set.of());
    when(this.users.me()).thenReturn(Mono.just(profile(true)));
    this.activityCounts(0, 2);

    StepVerifier.create(this.useCase.execute())
        .assertNext(context -> {
          assertThat(context.authenticated()).isTrue();
          assertThat(context.capabilities().canAddMedia()).isFalse();
          assertThat(context.capabilities().canManageLibraries()).isFalse();
          assertThat(context.activity().failed()).isEqualTo(2);
        })
        .verifyComplete();
  }

  @Test
  void usersServiceDownDegradesGracefullyWithoutKillingShell() {
    this.principalIs("pepe", Set.of());
    when(this.users.me())
        .thenReturn(Mono.error(new RuntimeException("users down")));
    this.activityCounts(0, 0);

    StepVerifier.create(this.useCase.execute())
        .assertNext(context -> {
          assertThat(context.authenticated()).isTrue();
          assertThat(context.user().username()).isEqualTo("pepe");
          // Conservador: identidad válida conserva uso básico.
          assertThat(context.capabilities().canAddMedia()).isTrue();
        })
        .verifyComplete();
  }
}
