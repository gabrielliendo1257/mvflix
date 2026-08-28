package com.gcorp.service.app.mvflix_movies.infrastructure.security;

import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.authentication.UserDetailsRepositoryReactiveAuthenticationManager;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.core.userdetails.MapReactiveUserDetailsService;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.util.matcher.ServerWebExchangeMatchers;
import reactor.core.publisher.Mono;

@Configuration
@Profile("!sandbox")
@EnableWebFluxSecurity
public class SecurityConfig {

  @Value("${services.authorization.url}")
  private String authorizationUrl;

  @Value("${security.oauth2.jwk-set-uri:}")
  private String jwkSetUriOverride;

  @Bean
  @Order(0)
  SecurityWebFilterChain actuatorSecurityWebFilterChain(
      ServerHttpSecurity http,
      @Value("${ACTUATOR_METRICS_USER:metrics}") String username,
      @Value("${ACTUATOR_METRICS_PASSWORD:change-me}") String password) {
    return http.securityMatcher(ServerWebExchangeMatchers.pathMatchers("/actuator/**"))
        .csrf(ServerHttpSecurity.CsrfSpec::disable)
        .authenticationManager(metricsAuthenticationManager(username, password))
        .httpBasic(org.springframework.security.config.Customizer.withDefaults())
        .authorizeExchange(exchanges -> exchanges
            .pathMatchers("/actuator/health", "/actuator/health/**").permitAll()
            .anyExchange().hasRole("METRICS"))
        .build();
  }

  @Bean
  SecurityWebFilterChain securityWebFilterChain(
      ServerHttpSecurity http, @Value("${security.oauth2.jwk-set-uri}") String jwkSetUri) {
    return http.csrf(ServerHttpSecurity.CsrfSpec::disable)
        .authorizeExchange(
            exchanges ->
                exchanges
                    .pathMatchers("/error")
                    .permitAll()
                    .pathMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html")
                    .permitAll()
                    .pathMatchers("/admin/outbox/**").hasRole("ADMIN")
                    .anyExchange()
                    .authenticated())
        .oauth2ResourceServer(
            resourceServer ->
                resourceServer.jwt(
                    jwt ->
                        jwt.jwkSetUri(jwkSetUri)
                            .jwtAuthenticationConverter(this.jwtAuthenticationConverter())))
        .build();
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
    var authorities = new ArrayList<GrantedAuthority>();
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

  private ReactiveAuthenticationManager metricsAuthenticationManager(String username, String password) {
    var user = User.withUsername(username)
        .password("{noop}" + password)
        .roles("METRICS")
        .build();
    return new UserDetailsRepositoryReactiveAuthenticationManager(
        new MapReactiveUserDetailsService(user));
  }
}
