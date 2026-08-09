package com.guille.media.reproductor.uploader.storage.infrastructure.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.convert.converter.Converter;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.oauth2.server.resource.authentication.ReactiveJwtAuthenticationConverterAdapter;
import org.springframework.security.web.server.SecurityWebFilterChain;
import reactor.core.publisher.Mono;

/**
 * Resource server configuration.
 *
 * <p>Valida los JWT emitidos por el authorization-service (endpoint {@code /oauth2/jwks} cuya
 * URL se resuelve desde {@code services.authorization.url}, red interna de docker-compose) y
 * traduce el claim {@code roles} a autoridades. El claim ya contiene el prefijo {@code ROLE_}
 * (por ejemplo {@code ROLE_ADMIN}), por lo que el converter no añade prefijo.
 */
@Configuration
@Profile("!sandbox")
@EnableWebFluxSecurity
public class SecurityConfiguration {

  @Value("${services.authorization.url}")
  private String authorizationUrl;

  @Value("${api.path.base}")
  private String apiPathBase;

  @Value("${security.oauth2.jwk-set-uri:}")
  private String jwkSetUriOverride;

  @Bean
  SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
    http.csrf(ServerHttpSecurity.CsrfSpec::disable)
        .authorizeExchange(
            exchanges ->
                exchanges
                    .pathMatchers("/ws")
                    .permitAll()
                    .pathMatchers(HttpMethod.POST, "/internal/minio/events")
                    .permitAll()
                    .pathMatchers(HttpMethod.POST, this.apiPathBase + "/storage/upload")
                    .hasRole("ADMIN")
                    .pathMatchers(
                        HttpMethod.GET,
                        this.apiPathBase + "/storage/users/*/quota")
                    .hasAuthority("SCOPE_storage.read")
                    .pathMatchers(HttpMethod.POST, this.apiPathBase + "/storage/upload/*/complete")
                    .authenticated()
                    .pathMatchers(HttpMethod.POST, this.apiPathBase + "/storage/streaming")
                    .authenticated()
                    .pathMatchers(
                        HttpMethod.GET,
                        this.apiPathBase + "/storage/uploads",
                        this.apiPathBase + "/storage/upload/*",
                        this.apiPathBase + "/storage/quota")
                    .authenticated()
                    .pathMatchers(
                        HttpMethod.POST,
                        this.apiPathBase + "/storage/upload",
                        this.apiPathBase + "/storage/upload/*/cancel")
                    .authenticated()
                    .anyExchange()
                    .denyAll())
        .oauth2ResourceServer(
            resourceServer ->
                resourceServer.jwt(
                    jwt ->
                        jwt.jwkSetUri(this.resolveJwkSetUri())
                            .jwtAuthenticationConverter(jwtAuthenticationConverter())));

    return http.build();
  }

  @Bean
  Converter<Jwt, Mono<AbstractAuthenticationToken>> jwtAuthenticationConverter() {
    var authoritiesConverter = new JwtGrantedAuthoritiesConverter(); // claim "scope" -> SCOPE_*
    var rolesConverter = new JwtGrantedAuthoritiesConverter();
    rolesConverter.setAuthoritiesClaimName("roles");
    rolesConverter.setAuthorityPrefix("");

    return jwt ->
        Mono.just(
            new org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken(
                jwt, mergeAuthorities(authoritiesConverter, rolesConverter, jwt)));
  }

  private java.util.List<org.springframework.security.core.GrantedAuthority> mergeAuthorities(
      JwtGrantedAuthoritiesConverter scopeConverter,
      JwtGrantedAuthoritiesConverter rolesConverter,
      Jwt jwt) {
    var authorities = new java.util.ArrayList<org.springframework.security.core.GrantedAuthority>();
    if (scopeConverter.convert(jwt) != null) {
      authorities.addAll(scopeConverter.convert(jwt));
    }
    if (rolesConverter.convert(jwt) != null) {
      authorities.addAll(rolesConverter.convert(jwt));
    }
    return authorities;
  }

  private String resolveJwkSetUri() {
    if (this.jwkSetUriOverride != null && !this.jwkSetUriOverride.isBlank()) {
      return this.jwkSetUriOverride;
    }
    return this.authorizationUrl + "/oauth2/jwks";
  }
}
