package com.guille.media.reproductor.users.infra.http;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.InMemoryReactiveClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.ReactiveClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.reactive.function.client.ServerOAuth2AuthorizedClientExchangeFilterFunction;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Cliente HTTP reactivo del user-service hacia el storage-service.
 *
 * <p>WebClient con {@code client_credentials} (identidad de máquina): el
 * storage-service valida el scope {@code storage.read} en su resource server.
 * El registro es estático (sin discovery OIDC) para que el contexto arranque
 * sin red: el token endpoint es el estándar del authorization-service.
 */
@Configuration
@Profile("!sandbox")
public class StorageOAuth2WebClientConfiguration {

    @Value("${security.oauth2.client-registration-id:users-app}")
    private String registrationId;

    @Value("${security.oauth2.client-id:users-service}")
    private String clientId;

    @Value("${security.oauth2.client-secret}")
    private String clientSecret;

    @Value("${services.authorization.url}")
    private String authorizationUrl;

    @Bean
    ReactiveClientRegistrationRepository storageClientRegistrationRepository() {
        ClientRegistration storageClient =
                ClientRegistration.withRegistrationId(this.registrationId)
                        .clientId(this.clientId)
                        .clientSecret(this.clientSecret)
                        .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
                        .tokenUri(this.authorizationUrl + "/oauth2/token")
                        .scope("storage.read")
                        .build();

        return new InMemoryReactiveClientRegistrationRepository(storageClient);
    }

    @Bean
    ServerOAuth2AuthorizedClientExchangeFilterFunction storageOauth2ClientCredentialsFilter(
            ReactiveClientRegistrationRepository clientRegistrationRepository) {
        var filter =
                new ServerOAuth2AuthorizedClientExchangeFilterFunction(
                        clientRegistrationRepository,
                        new InMemoryServerOAuth2AuthorizedClientRepository());
        filter.setDefaultClientRegistrationId(this.registrationId);
        return filter;
    }

    @Bean
    WebClient storageServiceWebClient(
            ServerOAuth2AuthorizedClientExchangeFilterFunction storageOauthFilter,
            @Value("${services.storage.url}") String storageUrl) {
        return WebClient.builder()
                .baseUrl(storageUrl)
                .filter(storageOauthFilter)
                .build();
    }
}