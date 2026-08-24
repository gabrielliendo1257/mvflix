package com.guille.media.bff.experience.shell.application;

import com.guille.media.bff.app.dto.UserProfile;
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
 * <p>Degradación: si users-service está caído, el shell sigue vivo con el
 * subject de sesión y capacidades conservadoras (sin bloquear la UI).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GetShellContext {

  private static final String ADMIN_ROLE = "ROLE_ADMIN";

  private final CurrentPrincipal principal;
  private final UsersWebPort users;
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
    Mono<UserProfile> profile =
        this.users
            .me()
            .onErrorResume(error -> {
              log.warn("shell: users-service no disponible, capacidades conservadoras: {}",
                  error.getMessage());
              return Mono.just(degradedProfile(identity));
            });
    return Mono.zip(profile, this.activitySummary.summaryFor(identity.subject()))
        .map(tuple -> contextFor(identity, tuple.getT1(), tuple.getT2()));
  }

  private static UserProfile degradedProfile(PrincipalIdentity identity) {
    return new UserProfile(null, identity.subject(), null, null, true, 0, false);
  }

  private static ShellContext contextFor(PrincipalIdentity identity,
      UserProfile profile, ShellActivity activity) {
    boolean blocked = profile.blocked();
    boolean enabled = profile.enabled();
    boolean isAdmin = identity.authorities().contains(ADMIN_ROLE);
    return new ShellContext(
        true,
        userOf(identity, profile),
        new ShellCapabilities(
            enabled && !blocked,
            enabled && !blocked,
            isAdmin),
        activity);
  }

  private static ShellUser userOf(PrincipalIdentity identity, UserProfile profile) {
    if (profile.username() == null) {
      // Perfil degradado: solo conocemos el subject de la sesión.
      return new ShellUser(null, identity.subject(), identity.subject(), null, null);
    }
    return new ShellUser(
        profile.id(),
        profile.username(),
        profile.username(),
        profile.email(),
        null);
  }
}
