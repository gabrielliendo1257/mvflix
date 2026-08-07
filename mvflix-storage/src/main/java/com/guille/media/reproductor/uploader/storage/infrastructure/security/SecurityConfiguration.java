package com.guille.media.reproductor.uploader.storage.infrastructure.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.ClientRegistrations;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;

import com.guille.media.reproductor.uploader.storage.app.service.ServiceLocator;

@Configuration
public class SecurityConfiguration {

    private final ServiceLocator serviceLocator;

    @Value(value = "${api.v2.path.base}")
    private String apiV2PathBase;

    public SecurityConfiguration(ServiceLocator serviceLocator) {
        this.serviceLocator = serviceLocator;
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) {
        http.csrf(AbstractHttpConfigurer::disable)
                .oauth2ResourceServer(oauth2ResourceConfigurer -> oauth2ResourceConfigurer
                        .jwt(jwtConfigurer -> jwtConfigurer
                                .jwkSetUri(this.serviceLocator + "/oauth2/jwks"))) // TODO Not hardcoding path jwks
                .authorizeHttpRequests(authorizaConfig -> authorizaConfig
                        .requestMatchers(HttpMethod.POST, this.apiV2PathBase + "/storage/upload").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, this.apiV2PathBase + "/storage/streaming").permitAll())
                .oauth2Login(Customizer.withDefaults());

        return http.build();
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
    ClientRegistrationRepository clientRegistrationRepository(ServiceLocator serviceLocator) {
        ClientRegistration movieClient = ClientRegistrations.fromIssuerLocation(
                serviceLocator.authorizationServer().toString())
                .registrationId("storage-app")
                .clientId("storage-service")
                .clientSecret("super-secret")
                .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
                .scope("users.read", "users.write")
                .build();

        return new InMemoryClientRegistrationRepository(movieClient);
    }
}
