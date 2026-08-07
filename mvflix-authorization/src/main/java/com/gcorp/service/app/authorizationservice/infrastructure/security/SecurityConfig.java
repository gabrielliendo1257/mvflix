package com.gcorp.service.app.authorizationservice.infrastructure.security;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.gcorp.service.app.authorizationservice.infrastructure.persistence.jpa.customer.CustomerEntity;
import com.gcorp.service.app.authorizationservice.infrastructure.persistence.jpa.customer.CustomerMapper;
import com.gcorp.service.app.authorizationservice.infrastructure.persistence.jpa.customer.CustomerRepository;
import com.gcorp.service.app.authorizationservice.models.Authorization;
import com.gcorp.service.app.authorizationservice.models.CustomerSecurity;
import com.gcorp.service.app.authorizationservice.models.Role;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.security.oauth2.server.authorization.token.JwtEncodingContext;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenCustomizer;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.client.RestTemplate;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    // @Value("${authorization.env.jwk.uri}")
    // private String jwkUrl;

    @Bean
    RestTemplate restTemplate() {
        return new RestTemplate();
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    @Order(value = 2)
    SecurityFilterChain configuration(HttpSecurity http) {
        http.csrf(AbstractHttpConfigurer::disable)
            // .oauth2ResourceServer(resourceServer ->
            //     resourceServer.jwt(jwtConfig ->
            //         jwtConfig.jwkSetUri(this.jwkUrl)
            //     )
            // )
            .authorizeHttpRequests(authorize ->
                authorize.anyRequest().authenticated()
            )
            .formLogin(Customizer.withDefaults());

        return http.build();
    }

    @Bean
    AuthenticationManager authenticationProvider(
        PasswordEncoder passwordEncoder,
        @Qualifier("userDetailService") UserDetailsService userDetailsService
    ) {
        DaoAuthenticationProvider daoAuthenticationProvider =
            new DaoAuthenticationProvider(userDetailsService);
        daoAuthenticationProvider.setPasswordEncoder(passwordEncoder);

        return new ProviderManager(daoAuthenticationProvider);
    }

    @Bean
    OAuth2TokenCustomizer<JwtEncodingContext> tokenCustomizer(
        @Value(value = "${spring.application.name}") String applicationName
    ) {
        return context -> {
            var authentication = context.getPrincipal();

            if (
                authentication instanceof
                    UsernamePasswordAuthenticationToken authenticationToken
            ) {
                if (context.getTokenType() == OAuth2TokenType.ACCESS_TOKEN) {
                    List<String> roles = authenticationToken
                        .getAuthorities()
                        .stream()
                        .map(GrantedAuthority::getAuthority)
                        .toList();

                    context.getClaims().audience(List.of(applicationName));
                    context.getClaims().subject(authenticationToken.getName());
                    context.getClaims().claim("roles", roles);

                    log.info("Roles: {}", roles);
                }
            }
        };
    }

    @Bean
    JwtAuthenticationConverter authenticationConverter() {
        var jwtConverter = new JwtGrantedAuthoritiesConverter();
        jwtConverter.setAuthorityPrefix("");
        jwtConverter.setAuthoritiesClaimName("roles");

        var jwtAuthConverter = new JwtAuthenticationConverter();
        jwtAuthConverter.setJwtGrantedAuthoritiesConverter(jwtConverter);

        return jwtAuthConverter;
    }

    @Bean
    UserDetailsService userDetailService(
        CustomerRepository accountrepository,
        CustomerMapper accountMapper
    ) {
        return username -> {
            CustomerEntity accountEntity = accountrepository
                .findSecurityCustomerByUsername(username)
                .orElseThrow(() ->
                    new UsernameNotFoundException("Ususario no encontrado.")
                );
            Set<Authorization> authorizations = accountEntity
                .getRole()
                .getAuthorities()
                .stream()
                .map(rol -> new Authorization(rol.getName()))
                .collect(Collectors.toSet());

            CustomerSecurity customerSecurity = CustomerSecurity.builder()
                .username(accountEntity.getUsername())
                .password(accountEntity.getPassword())
                .role(
                    new Role(
                        accountEntity.getRole().getRoleName(),
                        authorizations
                    )
                )
                .build();
            log.info("SecurityAccount after mapper: {}", customerSecurity);

            return customerSecurity;
        };
    }
}
