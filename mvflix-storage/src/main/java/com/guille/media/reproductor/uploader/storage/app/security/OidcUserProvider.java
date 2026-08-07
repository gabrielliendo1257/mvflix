package com.guille.media.reproductor.uploader.storage.app.security;

import lombok.RequiredArgsConstructor;

import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.client.RestClient;

import reactor.core.publisher.Mono;

@RequiredArgsConstructor
public class OidcUserProvider implements UserProvider {
    private final RestClient restClient;

    @Override
    public Mono<AuthenticatedUser> getAuthenticatedUser() {
        return ReactiveSecurityContextHolder.getContext()
                .switchIfEmpty(
                        Mono.error(
                                new AuthenticationCredentialsNotFoundException(
                                        "Authentication object is null")))
                .map(ctx -> ctx.getAuthentication())
                .flatMap(
                        authentication -> {
                            Object principal = authentication.getPrincipal();
                            if (principal instanceof OidcUser) {
                                OidcUser oidcUser = (OidcUser) principal;
                                return Mono.just(
                                        new AuthenticatedUser(
                                                oidcUser.getSubject(), oidcUser.getEmail()));
                            }
                            return Mono.empty();
                        });
    }
}
