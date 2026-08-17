package com.gcorp.service.app.mvflix_movies.app.security;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import reactor.core.publisher.Mono;

/**
 * Provee un usuario fijo en el perfil {@code sandbox}, donde no hay
 * authorization-service. Permite ejercitar el flujo completo del catalogo
 * (crear, completar, enriquecer, borrar) sin tokens JWT.
 */
@Component
@Profile("sandbox")
public class SandboxUserProvider implements UserProvider {

    private static final AuthenticatedUser SANDBOX_USER =
            new AuthenticatedUser("pepe", "pepe@mvflix.dev");

    @Override
    public Mono<AuthenticatedUser> getAuthenticatedUser() {
        return Mono.just(SANDBOX_USER);
    }
}