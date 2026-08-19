package com.guille.media.reproductor.uploader.storage.infrastructure.security;

import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.oauth2.server.resource.authentication.ReactiveJwtAuthenticationConverterAdapter;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.util.matcher.ServerWebExchangeMatchers;
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

  /**
   * Cadena para los endpoints internos ({@code /internal/**}): el webhook de MinIO no firma JWT
   * (envía el token como {@code Authorization: Bearer <webhook-token>}) y el filtro de resource
   * server lo rechazaría con 401 antes de llegar al controlador. Aquí no hay resource server.
   */
  @Bean
  @Order(0)
  SecurityWebFilterChain internalSecurityWebFilterChain(ServerHttpSecurity http) {
    http.securityMatcher(ServerWebExchangeMatchers.pathMatchers("/internal/**"))
        .csrf(ServerHttpSecurity.CsrfSpec::disable)
        .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
        .formLogin(ServerHttpSecurity.FormLoginSpec::disable)
        .authorizeExchange(authorize -> authorize.anyExchange().permitAll());
    return http.build();
  }

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
                    .authenticated()
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
                        HttpMethod.GET,
                        this.apiPathBase + "/storage/libraries",
                        this.apiPathBase + "/storage/libraries/*/files",
                        this.apiPathBase + "/storage/libraries/*/files/**")
                    .authenticated()
                    .pathMatchers(
                        HttpMethod.POST,
                        this.apiPathBase + "/storage/libraries")
                    .authenticated()
                    .pathMatchers(
                        HttpMethod.DELETE,
                        this.apiPathBase + "/storage/libraries/*")
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

  // No es @Bean a proposito: WebFlux registra todos los Converter beans en el
  // webFluxConversionService y una lambda no retiene la info generica (spring-framework#22509).
  Converter<Jwt, Mono<AbstractAuthenticationToken>> jwtAuthenticationConverter() {
    var authoritiesConverter = new JwtGrantedAuthoritiesConverter(); // claim "scope" -> SCOPE_*
    var rolesConverter = new JwtGrantedAuthoritiesConverter();
    rolesConverter.setAuthoritiesClaimName("roles");
    rolesConverter.setAuthorityPrefix("");

    return jwt ->
        Mono.just(
            new JwtAuthenticationToken(
                jwt, mergeAuthorities(authoritiesConverter, rolesConverter, jwt)));
  }

  private List<GrantedAuthority> mergeAuthorities(
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
