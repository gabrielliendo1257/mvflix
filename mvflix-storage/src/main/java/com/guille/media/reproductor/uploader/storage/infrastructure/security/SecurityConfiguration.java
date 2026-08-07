package com.guille.media.reproductor.uploader.storage.infrastructure.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.oauth2.server.resource.authentication.ReactiveJwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.ReactiveJwtGrantedAuthoritiesConverter;
import org.springframework.security.web.server.SecurityWebFilterChain;

import com.guille.media.reproductor.uploader.storage.app.service.ServiceLocator;

/**
 * Resource server configuration.
 *
 * <p>Valida los JWT emitidos por el authorization-service (endpoint
 * {@code /oauth2/jwks} resuelto vía discovery) y traduce el claim
 * {@code roles} a autoridades. El claim ya contiene el prefijo {@code ROLE_}
 * (por ejemplo {@code ROLE_ADMIN}), por lo que el converter no añade prefijo.
 */
@Configuration
@EnableWebFluxSecurity
public class SecurityConfiguration {

    private final ServiceLocator serviceLocator;

    @Value("${api.path.base}")
    private String apiPathBase;

    @Value("${security.oauth2.jwk-set-uri:}")
    private String jwkSetUriOverride;

    public SecurityConfiguration(ServiceLocator serviceLocator) {
        this.serviceLocator = serviceLocator;
    }

    @Bean
    SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        http.csrf(ServerHttpSecurity.CsrfSpec::disable)
                .authorizeExchange(exchanges -> exchanges
                        .pathMatchers("/ws").permitAll()
                        .pathMatchers(HttpMethod.POST, this.apiPathBase + "/storage/upload")
                        .hasRole("ADMIN")
                        .pathMatchers(HttpMethod.POST, this.apiPathBase + "/storage/upload/*/complete")
                        .authenticated()
                        .pathMatchers(HttpMethod.POST, this.apiPathBase + "/storage/streaming")
                        .authenticated()
                        .anyExchange().denyAll())
                .oauth2ResourceServer(resourceServer -> resourceServer
                        .jwt(jwt -> jwt
                                .jwkSetUri(this.resolveJwkSetUri())
                                .jwtAuthenticationConverter(jwtAuthenticationConverter())));

        return http.build();
    }

    @Bean
    ReactiveJwtAuthenticationConverter jwtAuthenticationConverter() {
        var authoritiesConverter = new ReactiveJwtGrantedAuthoritiesConverter();
        authoritiesConverter.setAuthoritiesClaimName("roles");
        authoritiesConverter.setAuthorityPrefix("");

        var jwtAuthenticationConverter = new ReactiveJwtAuthenticationConverter();
        jwtAuthenticationConverter.setJwtGrantedAuthoritiesConverter(authoritiesConverter);

        return jwtAuthenticationConverter;
    }

    private String resolveJwkSetUri() {
        if (this.jwkSetUriOverride != null && !this.jwkSetUriOverride.isBlank()) {
            return this.jwkSetUriOverride;
        }
        return this.serviceLocator.authorizationServer() + "/oauth2/jwks";
    }
}
