package com.guille.media.bff.experience.shell.application;

import com.guille.media.bff.app.dto.QuotaSnapshot;
import com.guille.media.bff.app.dto.UserProfile;
import com.guille.media.bff.app.ports.StorageWebClient;
import com.guille.media.bff.app.ports.UsersWebPort;
import com.guille.media.bff.experience.shell.application.port.CurrentPrincipal;
import com.guille.media.bff.experience.shell.application.port.CurrentPrincipal.PrincipalIdentity;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;

import reactor.core.publisher.Mono;

/**
 * Bootstrap de la interfaz: ¿quién está usando la aplicación y qué
 * capacidades/contexto necesita para construir la navegación?
 *
 * <p>Fuentes de verdad independientes: identidad/cuenta (users), roles (claim
 * del token emitido por el IdP), consumo de cuota (storage). Cada una se
 * degrada por separado: si users o storage están caídos el shell sigue vivo,
 * conservador en capacidades y sin indicadores falsos.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GetShellContext {

  private static final String ADMIN_ROLE = "ROLE_ADMIN";

  private final CurrentPrincipal principal;
  private final UsersWebPort users;
  private final StorageWebClient storage;
  private final ActivitySummary activitySummary;

  public Mono<ShellContext> execute() {
    return this.principal
        .current()
        .flatMap(this::buildFor)
        // Un anónimo TAMBIÉN recibe shell: capabilities en false para que la
        // UI pinte el estado de sign-in.
        .defaultIfEmpty(ShellContext.anonymous());
  }

  private Mono<ShellContext> buildFor(PrincipalIdentity identity) {
    var profile = this.users
        .me()
        .onErrorResume(error -> {
          log.warn("shell: users-service no disponible, capacidades conservadoras: {}",
              error.getMessage());
          return Mono.just(degradedProfile(identity));
        });
    var quota = this.storage
        .quota()
        .map(GetShellContext::quotaOf)
        .onErrorResume(error -> {
          log.warn("shell: cuota no disponible: {}", error.getMessage());
          return Mono.just(ShellQuota.unavailable());
        })
        .defaultIfEmpty(ShellQuota.unavailable());
    return Mono.zip(profile, this.activitySummary.summaryFor(identity.subject()), quota)
        .map(tuple -> contextFor(identity, tuple.getT1(), tuple.getT2(), tuple.getT3()));
  }

  private static UserProfile degradedProfile(PrincipalIdentity identity) {
    return new UserProfile(
        null, identity.subject(), null, null, null, null, true, 0, false);
  }

  private static ShellQuota quotaOf(QuotaSnapshot snapshot) {
    long limit = snapshot.quotaBytes();
    int percent = limit > 0
        ? (int) Math.min(100, Math.round(100.0 * snapshot.usedBytes() / limit))
        : 0;
    return new ShellQuota(true, snapshot.usedBytes(), limit, percent);
  }

  private static ShellContext contextFor(PrincipalIdentity identity,
      UserProfile profile, ShellActivity activity, ShellQuota quota) {
    boolean usable = profile.enabled() && !profile.blocked();
    boolean isAdmin = identity.authorities().contains(ADMIN_ROLE);
    return new ShellContext(
        true,
        userOf(identity, profile),
        isAdmin ? ShellCapabilities.admin(usable) : ShellCapabilities.accountOnly(usable),
        activity,
        quota);
  }

  private static ShellUser userOf(PrincipalIdentity identity, UserProfile profile) {
    if (profile.username() == null) {
      // Perfil degradado: solo conocemos el subject de la sesión.
      return new ShellUser(null, identity.subject(), identity.subject(), null, null);
    }
    // displayName/avatarUrl reales de users; null = la UI usa username/iniciales.
    return new ShellUser(
        profile.id(),
        profile.username(),
        profile.displayName(),
        profile.email(),
        profile.avatarUrl());
  }
}
