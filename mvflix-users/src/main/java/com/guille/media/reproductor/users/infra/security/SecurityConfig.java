package com.guille.media.reproductor.users.infra.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.authentication.UserDetailsRepositoryReactiveAuthenticationManager;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity.CsrfSpec;
import org.springframework.security.config.web.server.ServerHttpSecurity.HttpBasicSpec;
import org.springframework.security.core.userdetails.MapReactiveUserDetailsService;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.web.server.SecurityWebFilterChain;

@Configuration
@Profile("!sandbox")
@EnableWebFluxSecurity
public class SecurityConfig {

    @Bean
    @Order(0)
    SecurityWebFilterChain actuatorSecurityWebFilterChain(
            ServerHttpSecurity http,
            @Value("${ACTUATOR_METRICS_USER:metrics}") String username,
            @Value("${ACTUATOR_METRICS_PASSWORD:change-me}") String password) {
        return http.securityMatcher(org.springframework.security.web.server.util.matcher.ServerWebExchangeMatchers
                        .pathMatchers("/actuator/**"))
                .csrf(CsrfSpec::disable)
                .authenticationManager(metricsAuthenticationManager(username, password))
                .httpBasic(org.springframework.security.config.Customizer.withDefaults())
                .authorizeExchange(authorize -> authorize.anyExchange().hasRole("METRICS"))
                .build();
    }

    @Bean
    SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http) {
        http.csrf(CsrfSpec::disable)
                .httpBasic(HttpBasicSpec::disable)
                .oauth2ResourceServer(spec -> spec.jwt(Customizer.withDefaults()))
                .authorizeExchange(
                        authorizeSpec ->
                                authorizeSpec
                                        .pathMatchers(HttpMethod.POST, "/api/v1/users")
                                        .permitAll()
                                        .pathMatchers("/api/v1/users/me")
                                        .authenticated()
                                        .pathMatchers(HttpMethod.POST, "/api/v1/users/me/violations")
                                        .authenticated()
                                        .pathMatchers("/api/v1/users/*/plan")
                                        .hasAuthority("SCOPE_users.write")
                                        .anyExchange()
                                        .denyAll());

        return http.build();
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
