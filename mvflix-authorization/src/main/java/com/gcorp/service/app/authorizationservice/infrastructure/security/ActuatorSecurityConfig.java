package com.gcorp.service.app.authorizationservice.infrastructure.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class ActuatorSecurityConfig {

    @Bean
    @Order(1)
    SecurityFilterChain actuatorSecurityFilterChain(
        HttpSecurity http,
        @Value("${ACTUATOR_METRICS_USER:metrics}") String username,
        @Value("${ACTUATOR_METRICS_PASSWORD:change-me}") String password
    ) throws Exception {
        var provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(new InMemoryUserDetailsManager(
            User.withUsername(username)
                .password("{noop}" + password)
                .roles("METRICS")
                .build()));
        provider.setPasswordEncoder(PasswordEncoderFactories.createDelegatingPasswordEncoder());

        http.securityMatcher("/actuator/**")
            .csrf(AbstractHttpConfigurer::disable)
            .authenticationManager(new ProviderManager(provider))
            .httpBasic(Customizer.withDefaults())
            .authorizeHttpRequests(authorize -> authorize
                .requestMatchers("/actuator/health", "/actuator/health/**").permitAll()
                .anyRequest().hasRole("METRICS"));
        return http.build();
    }
}
