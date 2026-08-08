package com.guille.media.reproductor.uploader.storage.app.security;

import lombok.RequiredArgsConstructor;

import org.springframework.context.annotation.Profile;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import reactor.core.publisher.Mono;

/**
 * Extrae el usuario autenticado del contexto reactivo de seguridad.
 *
 * <p>Soporta tanto {@link Jwt} (resource server, caso normal) como
 * {@link OidcUser} (flujo de login OIDC). Si el principal no es ninguno de
 * los dos, falla rápido en lugar de devolver vacío.
 */
@Component
@Profile("!sandbox")
@RequiredArgsConstructor
public class JwtUserProvider implements UserProvider {

    @Override
    public Mono<AuthenticatedUser> getAuthenticatedUser() {
        return ReactiveSecurityContextHolder.getContext()
                .map(SecurityContext::getAuthentication)
                .flatMap(this::toAuthenticatedUser);
    }

    private Mono<AuthenticatedUser> toAuthenticatedUser(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return Mono.error(new AuthenticationCredentialsNotFoundException(
                    "No authenticated user"));
        }

        Object principal = authentication.getPrincipal();

        if (principal instanceof Jwt jwt) {
            return Mono.just(new AuthenticatedUser(
                    jwt.getSubject(), jwt.getClaimAsString("email")));
        }

        if (principal instanceof OidcUser oidcUser) {
            return Mono.just(new AuthenticatedUser(
                    oidcUser.getSubject(), oidcUser.getEmail()));
        }

        return Mono.error(new AuthenticationCredentialsNotFoundException(
                "Unsupported principal type: "
                        + (principal == null ? "null" : principal.getClass().getName())));
    }
}
