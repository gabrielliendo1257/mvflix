package com.guille.media.reproductor.users.infra.security;

import org.springframework.context.annotation.Profile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;

import reactor.core.publisher.Mono;

/**
 * En el perfil {@code sandbox} no hay authorization-service, así que siembra
 * un usuario fijo en el SecurityContext: los endpoints que leen el principal
 * (p. ej. GET /api/v1/users/me) funcionan igual que con JWT.
 */
@Component
@Profile("sandbox")
public class SandboxAuthenticationFilter implements WebFilter {

    private static final String SANDBOX_USERNAME = "pepe";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        var authentication =
                new UsernamePasswordAuthenticationToken(SANDBOX_USERNAME, null, java.util.List.of());
        var context = new SecurityContextImpl(authentication);
        return chain.filter(exchange)
                .contextWrite(ReactiveSecurityContextHolder.withSecurityContext(Mono.just(context)));
    }
}