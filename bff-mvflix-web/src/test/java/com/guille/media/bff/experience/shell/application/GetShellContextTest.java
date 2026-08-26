package com.guille.media.bff.experience.shell.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.guille.media.bff.app.dto.QuotaSnapshot;
import com.guille.media.bff.app.dto.UserProfile;
import com.guille.media.bff.app.ports.StorageWebClient;
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
  private final StorageWebClient storage = mock(StorageWebClient.class);
  private final ActivitySummary activity = mock(ActivitySummary.class);

  private final GetShellContext useCase =
      new GetShellContext(this.principal, this.users, this.storage, this.activity);

  private void principalIs(String subject, Set<String> authorities) {
    when(this.principal.current())
        .thenReturn(Mono.just(new PrincipalIdentity(subject, authorities)));
  }

  private void anonymousPrincipal() {
    when(this.principal.current()).thenReturn(Mono.empty());
  }

  private UserProfile profile(boolean blocked) {
    return new UserProfile("5bb", "Admin", "Admin Dev", "http://a/img.png",
        "admin@mvflix.local", "ENTERPRISE", true, 0, blocked);
  }

  private void activityCounts(long running, long failed) {
    when(this.activity.summaryFor(org.mockito.ArgumentMatchers.anyString()))
        .thenReturn(Mono.just(new ShellActivity(running, failed)));
  }

  private void quota(long used, long limit) {
    when(this.storage.quota())
        .thenReturn(Mono.just(new QuotaSnapshot("Admin", limit, used, limit - used)));
  }

  private void quotaDown() {
    when(this.storage.quota()).thenReturn(Mono.error(new RuntimeException("storage down")));
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
          assertThat(context.quota()).isNull();
        })
        .verifyComplete();

    org.mockito.Mockito.verifyNoInteractions(this.users, this.activity, this.storage);
  }

  @Test
  void authenticatedNormalUserSeesOwnActivityQuotaAndAccountCaps() {
    this.principalIs("pepe", Set.of());
    when(this.users.me()).thenReturn(Mono.just(profile(false)));
    this.activityCounts(1, 0);
    this.quota(2500L, 10000L);

    StepVerifier.create(this.useCase.execute())
        .assertNext(context -> {
          assertThat(context.authenticated()).isTrue();
          assertThat(context.user().id()).isEqualTo("5bb");
          assertThat(context.user().displayName()).isEqualTo("Admin Dev");
          assertThat(context.user().avatarUrl()).isEqualTo("http://a/img.png");
          assertThat(context.capabilities().canAddMedia()).isTrue();
          // Las bibliotecas locales son del operador: solo el admin las gestiona.
          assertThat(context.capabilities().canManageLibraries()).isFalse();
          assertThat(context.capabilities().canAccessAdmin()).isFalse();
          assertThat(context.capabilities().canModerateCatalog()).isFalse();
          assertThat(context.activity().running()).isEqualTo(1);
          assertThat(context.quota().available()).isTrue();
          assertThat(context.quota().usedPercent()).isEqualTo(25);
        })
        .verifyComplete();
  }

  @Test
  void adminRoleGrantsAllAdminCapabilities() {
    this.principalIs("admin", Set.of("ROLE_ADMIN"));
    when(this.users.me()).thenReturn(Mono.just(profile(false)));
    this.activityCounts(0, 0);
    this.quota(0L, 10000L);

    StepVerifier.create(this.useCase.execute())
        .assertNext(context -> {
          var caps = context.capabilities();
          assertThat(caps.canAccessAdmin()).isTrue();
          assertThat(caps.canManageAnyLibrary()).isTrue();
          assertThat(caps.canModerateCatalog()).isTrue();
          assertThat(caps.canViewAllActivity()).isTrue();
        })
        .verifyComplete();
  }

  @Test
  void blockedUserLosesProductCapabilitiesButKeepsIdentity() {
    this.principalIs("pepe", Set.of());
    when(this.users.me()).thenReturn(Mono.just(profile(true)));
    this.activityCounts(0, 2);
    this.quota(1L, 10L);

    StepVerifier.create(this.useCase.execute())
        .assertNext(context -> {
          assertThat(context.authenticated()).isTrue();
          assertThat(context.capabilities().canAddMedia()).isFalse();
          assertThat(context.capabilities().canManageLibraries()).isFalse();
          // El rol no desaparece por bloqueo, pero la cuenta no es usable.
          assertThat(context.capabilities().canAccessAdmin()).isFalse();
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
    this.quota(1L, 2L);

    StepVerifier.create(this.useCase.execute())
        .assertNext(context -> {
          assertThat(context.authenticated()).isTrue();
          assertThat(context.user().username()).isEqualTo("pepe");
          // Conservador: identidad válida conserva uso básico.
          assertThat(context.capabilities().canAddMedia()).isTrue();
          assertThat(context.quota().available()).isTrue();
        })
        .verifyComplete();
  }

  @Test
  void storageDownHidesQuotaIndicatorWithoutBreakingShell() {
    this.principalIs("pepe", Set.of());
    when(this.users.me()).thenReturn(Mono.just(profile(false)));
    this.activityCounts(0, 0);
    this.quotaDown();

    StepVerifier.create(this.useCase.execute())
        .assertNext(context -> {
          assertThat(context.authenticated()).isTrue();
          assertThat(context.quota().available()).isFalse();
          assertThat(context.quota().usedBytes()).isNull();
          assertThat(context.capabilities().canAddMedia()).isTrue();
        })
        .verifyComplete();
  }
}
