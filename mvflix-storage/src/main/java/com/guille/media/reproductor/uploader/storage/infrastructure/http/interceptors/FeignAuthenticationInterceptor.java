package com.guille.media.reproductor.uploader.storage.infrastructure.http.interceptors;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import lombok.RequiredArgsConstructor;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.security.oauth2.client.OAuth2AuthorizeRequest;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.stereotype.Component;

@Component
@Profile("!sandbox")
@RequiredArgsConstructor
public class FeignAuthenticationInterceptor implements RequestInterceptor {

    private final OAuth2AuthorizedClientManager oAuth2AuthorizedClientManager;

    @Value("${security.oauth2.client-registration-id:storage-app}")
    private String registrationId;

    @Value("${security.oauth2.client-principal:storage-service}")
    private String principal;

    private String getToken() {
        OAuth2AuthorizeRequest authorizeRequest = OAuth2AuthorizeRequest
                .withClientRegistrationId(this.registrationId)
                .principal(this.principal)
                .build();

        OAuth2AuthorizedClient client =
                this.oAuth2AuthorizedClientManager.authorize(authorizeRequest);

        if (client == null) {
            throw new IllegalStateException("Unable to authorize client: " + this.registrationId);
        }
        return client.getAccessToken().getTokenValue();
    }

    @Override
    public void apply(RequestTemplate requestTemplate) {
        requestTemplate.header("Authorization", "Bearer " + this.getToken());
    }
}
