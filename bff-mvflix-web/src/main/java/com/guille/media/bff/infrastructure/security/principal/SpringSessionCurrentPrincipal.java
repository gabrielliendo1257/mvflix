package com.guille.media.bff.infrastructure.security.principal;

import com.guille.media.bff.experience.shell.application.port.CurrentPrincipal;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.stereotype.Component;

import reactor.core.publisher.Mono;

import java.util.Set;
import java.util.stream.Collectors;

/** Adapter sobre el mecanismo real: Spring Security reactivo de la sesión. */
@Component
public class SpringSessionCurrentPrincipal implements CurrentPrincipal {

    @Override
    public Mono<PrincipalIdentity> current() {
        return ReactiveSecurityContextHolder.getContext()
            .map(SecurityContext::getAuthentication)
            .filter(Authentication::isAuthenticated)
            .map(auth -> new PrincipalIdentity(
                auth.getName(),
                auth.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .collect(Collectors.toUnmodifiableSet())));
    }
}
