package com.guille.media.bff.experience.shell.application.port;

import java.util.Set;

import reactor.core.publisher.Mono;

/**
 * Identidad del usuario de la sesión web. Puerto del contexto Shell: el
 * mecanismo (Spring Security / ReactiveSecurityContextHolder) vive en un
 * adapter de infraestructura.
 *
 * <p>Vacío = sin sesión autenticada (navegación anónima).
 */
public interface CurrentPrincipal {

    /** Identidad actual: subject + autoridades crudas del token de sesión. */
    Mono<PrincipalIdentity> current();

    record PrincipalIdentity(String subject, Set<String> authorities) {}
}
