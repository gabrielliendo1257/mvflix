package com.guille.media.reproductor.uploader.storage.infrastructure.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
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
 * <p>Valida los JWT emitidos por el authorization-service (endpoint {@code /oauth2/jwks} resuelto
 * vía discovery) y traduce el claim {@code roles} a autoridades. El claim ya contiene el prefijo
 * {@code ROLE_} (por ejemplo {@code ROLE_ADMIN}), por lo que el converter no añade prefijo.
 */
@Configuration
@EnableWebFluxSecurity
public class SecurityConfiguration {

  private final DiscoveryClient discoveryClient;

  @Value("${api.path.base}")
  private String apiPathBase;

  @Value("${security.oauth2.jwk-set-uri:}")
  private String jwkSetUriOverride;

  public SecurityConfiguration(DiscoveryClient discoveryClient) {
    this.discoveryClient = discoveryClient;
  }

  @Bean
  SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
    http.csrf(ServerHttpSecurity.CsrfSpec::disable)
        .authorizeExchange(
            exchanges ->
                exchanges
                    .pathMatchers("/ws")
                    .permitAll()
                    .pathMatchers(HttpMethod.POST, this.apiPathBase + "/storage/upload")
                    .hasRole("ADMIN")
                    .pathMatchers(HttpMethod.POST, this.apiPathBase + "/storage/upload/*/complete")
                    .authenticated()
                    .pathMatchers(HttpMethod.POST, this.apiPathBase + "/storage/streaming")
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
    var authoritiesConverter = new JwtGrantedAuthoritiesConverter();
    authoritiesConverter.setAuthoritiesClaimName("roles");
    authoritiesConverter.setAuthorityPrefix("");

    var jwtAuthenticationConverter = new JwtAuthenticationConverter();
    jwtAuthenticationConverter.setJwtGrantedAuthoritiesConverter(authoritiesConverter);

    return new ReactiveJwtAuthenticationConverterAdapter(jwtAuthenticationConverter);
  }

  private String resolveJwkSetUri() {
    if (this.jwkSetUriOverride != null && !this.jwkSetUriOverride.isBlank()) {
      return this.jwkSetUriOverride;
    }
    var instances = this.discoveryClient.getInstances("authorization-service");
    if (instances.isEmpty()) {
      throw new IllegalStateException("No instances registered for authorization-service");
    }
    return instances.get(0).getUri() + "/oauth2/jwks";
  }
}
