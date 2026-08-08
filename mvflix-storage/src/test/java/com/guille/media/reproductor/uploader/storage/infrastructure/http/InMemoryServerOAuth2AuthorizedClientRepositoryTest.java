package com.guille.media.reproductor.uploader.storage.infrastructure.http;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2AccessToken.TokenType;

class InMemoryServerOAuth2AuthorizedClientRepositoryTest {

    private final InMemoryServerOAuth2AuthorizedClientRepository repository =
            new InMemoryServerOAuth2AuthorizedClientRepository();

    private ClientRegistration registration;

    @BeforeEach
    void setUp() {
        this.registration = ClientRegistration.withRegistrationId("storage-app")
                .clientId("storage-service")
                .clientSecret("secret")
                .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
                .tokenUri("http://localhost/token")
                .build();
    }

    private OAuth2AuthorizedClient authorizedClient() {
        return new OAuth2AuthorizedClient(this.registration, "storage-service",
                new OAuth2AccessToken(TokenType.BEARER, "token-1", null, null));
    }

    @Test
    void savesAndLoadsClientByRegistrationId() {
        OAuth2AuthorizedClient client = this.authorizedClient();

        this.repository.saveAuthorizedClient(client, null, null).block();

        OAuth2AuthorizedClient loaded =
                this.repository.loadAuthorizedClient("storage-app", null, null).block();
        assertNotNull(loaded);
        assertEquals("token-1", loaded.getAccessToken().getTokenValue());
    }

    @Test
    void loadReturnsEmptyWhenNotCached() {
        assertNull(this.repository.loadAuthorizedClient("unknown", null, null).block());
    }

    @Test
    void removeClearsCachedClient() {
        this.repository.saveAuthorizedClient(this.authorizedClient(), null, null).block();

        this.repository.removeAuthorizedClient("storage-app", null, null).block();

        assertNull(this.repository.loadAuthorizedClient("storage-app", null, null).block());
    }

    @Test
    void sameRegistrationWithDifferentPrincipalIsNotShared() {
        Authentication principalA = new UsernamePasswordAuthenticationToken("svc-a", "x");
        Authentication principalB = new UsernamePasswordAuthenticationToken("svc-b", "x");

        this.repository.saveAuthorizedClient(this.authorizedClient(), principalA, null).block();

        assertNull(this.repository.loadAuthorizedClient("storage-app", principalB, null).block());
    }
}