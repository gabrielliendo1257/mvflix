package com.guille.media.reproductor.uploader.storage.infrastructure.http;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.oauth2.client.AuthorizedClientServiceOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.InMemoryOAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientProvider;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientProviderBuilder;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.ClientRegistrations;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.core.AuthorizationGrantType;

/**
 * Cliente OAuth2 usado por Feign para llamar al user-service.
 *
 * <p>No es el resource server: es la identidad de máquina del storage-service
 * (grant {@code client_credentials}) registrada en el authorization-service
 * con el client id {@code storage-service}.
 */
@Configuration
@Profile("!sandbox")
public class OAuth2ClientConfiguration {

    @Value("${security.oauth2.client-registration-id:storage-app}")
    private String registrationId;

    @Value("${security.oauth2.client-id:storage-service}")
    private String clientId;

    @Value("${security.oauth2.client-secret}")
    private String clientSecret;

    @Value("${services.authorization.url}")
    private String authorizationUrl;

    @Bean
    ClientRegistrationRepository clientRegistrationRepository() {
        ClientRegistration storageClient = ClientRegistrations
                .fromIssuerLocation(this.authorizationUrl)
                .registrationId(this.registrationId)
                .clientId(this.clientId)
                .clientSecret(this.clientSecret)
                .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
                .scope("users.read", "users.write")
                .build();

        return new InMemoryClientRegistrationRepository(storageClient);
    }

    @Bean
    OAuth2AuthorizedClientService authorizedClientService(
            ClientRegistrationRepository clientRegistrationRepository) {
        return new InMemoryOAuth2AuthorizedClientService(clientRegistrationRepository);
    }

    @Bean
    OAuth2AuthorizedClientManager authorizedClientManager(
            ClientRegistrationRepository clientRegistrationRepository,
            OAuth2AuthorizedClientService authorizedClientService) {
        OAuth2AuthorizedClientProvider authorizedClientProvider =
                OAuth2AuthorizedClientProviderBuilder.builder()
                        .clientCredentials()
                        .build();

        var authorizedClientManager = new AuthorizedClientServiceOAuth2AuthorizedClientManager(
                clientRegistrationRepository, authorizedClientService);
        authorizedClientManager.setAuthorizedClientProvider(authorizedClientProvider);

        return authorizedClientManager;
    }
}
