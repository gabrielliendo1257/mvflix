package com.guille.media.bff.infrastructure.security;

import java.util.Arrays;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.client.registration.ReactiveClientRegistrationRepository;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.ServerAuthenticationEntryPoint;
import org.springframework.security.web.server.authentication.RedirectServerAuthenticationEntryPoint;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;
import org.springframework.web.server.ServerWebExchange;

import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;

/**
 * Seguridad del BFF: patrón oauth2-client (el navegador nunca ve tokens).
 *
 * <p>Sin sesión: si el cliente espera HTML (navegador) se redirige al authorize del IdP;
 * para el resto (API/curl/Postman) se responde 401 JSON para que el front arranque el login.
 */
@Configuration
@EnableWebFluxSecurity
public class WebSecurityConfig {

	@Value("${services.authorization.url}")
	private String authorizationUrl;

	@Value("${security.oauth2.jwk-set-uri}")
	private String jwkSetUriOverride;

  private static final String AUTHORIZATION_URI = "/oauth2/authorization/movie-app";

  @Bean
  SecurityWebFilterChain securityWebFilterChain(
      ServerHttpSecurity http, ReactiveClientRegistrationRepository clientRegistrations) {
    return http.csrf(ServerHttpSecurity.CsrfSpec::disable)
        .authorizeExchange(
            exchanges ->
                exchanges
                    .pathMatchers("/web/session", "/login/**", "/oauth2/**", "/error", "/favicon.ico")
                    .permitAll()
                    .pathMatchers("/web/**")
                    .authenticated()
                    .anyExchange()
                    .permitAll())
        //.exceptionHandling(handling -> handling.authenticationEntryPoint(delegatingEntryPoint()))
        .oauth2ResourceServer(oauth2ResourceServer -> oauth2ResourceServer
					.jwt(jwtSpec -> jwtSpec
						.jwkSetUri(this.resolveJwkSetUri())
						.jwtAuthenticationConverter(this.jwtAuthenticationConverter())
					)
				)
        .oauth2Login(Customizer.withDefaults())
        .build();
  }

  /** Navegador (acepta HTML) -> redirect al authorize del IdP; resto -> 401 JSON. */
  private ServerAuthenticationEntryPoint delegatingEntryPoint() {
    return (exchange, ex) -> {
      var accept = exchange.getRequest().getHeaders().getAccept();
      boolean wantsHtml =
          accept != null
              && accept.stream()
                  .anyMatch(
                      mediaType ->
                          "text".equalsIgnoreCase(mediaType.getType())
                              && "html".equalsIgnoreCase(mediaType.getSubtype()));
      if (wantsHtml) {
        return new RedirectServerAuthenticationEntryPoint(AUTHORIZATION_URI).commence(exchange, ex);
      }
      return new Json401EntryPoint().commence(exchange, ex);
    };
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

	@Bean
	UrlBasedCorsConfigurationSource corsConfigurationSource() {
		CorsConfiguration configuration = new CorsConfiguration();
		configuration.setAllowedOrigins(Arrays.asList("http://127.0.0.1:4200"));
		configuration.setAllowedMethods(Arrays.asList("GET","POST"));
		configuration.setAllowCredentials(true);
		UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
		source.registerCorsConfiguration("/**", configuration);
		return source;
	}

  /** 401 en JSON para que el front distinga "sin sesión" y arranque el login. */
  private static final class Json401EntryPoint implements ServerAuthenticationEntryPoint {

    @Override
    public Mono<Void> commence(ServerWebExchange exchange, AuthenticationException ex) {
      exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
      exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
      return exchange
          .getResponse()
          .writeWith(
              Mono.just(
                  exchange
                      .getResponse()
                      .bufferFactory()
                      .wrap("{\"error\":\"unauthorized\"}".getBytes(StandardCharsets.UTF_8))));
    }
  }
}