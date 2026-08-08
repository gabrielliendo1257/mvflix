package com.guille.media.reproductor.uploader.storage.infrastructure.http;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrations;
import org.springframework.security.oauth2.client.registration.InMemoryReactiveClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.ReactiveClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.reactive.function.client.ServerOAuth2AuthorizedClientExchangeFilterFunction;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Cliente HTTP reactivo del storage-service hacia el user-service.
 *
 * <p>WebClient con {@code client_credentials} (identidad de máquina registrada
 * en el authorization-service). El filtro OAuth2 de Spring Security gestiona
 * la obtención y refresco del token de forma reactiva, sin bloquear el
 * event loop de WebFlux — a diferencia de Feign, que usa I/O bloqueante.
 */
@Configuration
@Profile("!sandbox")
public class OAuth2WebClientConfiguration {

    @Value("${security.oauth2.client-registration-id:storage-app}")
    private String registrationId;

    @Value("${security.oauth2.client-id:storage-service}")
    private String clientId;

    @Value("${security.oauth2.client-secret}")
    private String clientSecret;

    @Value("${services.authorization.url}")
    private String authorizationUrl;

    @Bean
    ReactiveClientRegistrationRepository reactiveClientRegistrationRepository() {
        ClientRegistration storageClient = ClientRegistrations
                .fromIssuerLocation(this.authorizationUrl)
                .registrationId(this.registrationId)
                .clientId(this.clientId)
                .clientSecret(this.clientSecret)
                .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
                .scope("users.read", "users.write")
                .build();

        return new InMemoryReactiveClientRegistrationRepository(storageClient);
    }

    @Bean
    ServerOAuth2AuthorizedClientExchangeFilterFunction oauth2ClientCredentialsFilter(
            ReactiveClientRegistrationRepository clientRegistrationRepository) {
        var filter = new ServerOAuth2AuthorizedClientExchangeFilterFunction(
                clientRegistrationRepository,
                new InMemoryServerOAuth2AuthorizedClientRepository());
        filter.setDefaultClientRegistrationId(this.registrationId);
        return filter;
    }

    @Bean
    WebClient userServiceWebClient(
            ServerOAuth2AuthorizedClientExchangeFilterFunction oauth2ClientCredentialsFilter,
            @Value("${services.users.url}") String usersUrl) {
        return WebClient.builder()
                .baseUrl(usersUrl)
                .filter(oauth2ClientCredentialsFilter)
                .build();
    }
}