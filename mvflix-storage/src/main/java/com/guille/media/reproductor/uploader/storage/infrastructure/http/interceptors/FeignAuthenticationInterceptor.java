package com.guille.media.reproductor.uploader.storage.infrastructure.http.interceptors;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.client.OAuth2AuthorizeRequest;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class FeignAuthenticationInterceptor implements RequestInterceptor {
    private final OAuth2AuthorizedClientManager oAuth2AuthorizedClientManager;

    private String getToken() {
        OAuth2AuthorizeRequest authorizeRequest = OAuth2AuthorizeRequest
                .withClientRegistrationId("user-service") // TODO Not hardcoding
                .principal("storage-service")
                .build();

        OAuth2AuthorizedClient client = this.oAuth2AuthorizedClientManager.authorize(authorizeRequest);

        if (client == null) {
            throw new IllegalStateException("Unable to authorize client");
        }
        return client.getAccessToken().getTokenValue();
    }

    @Override
    public void apply(RequestTemplate requestTemplate) {
        requestTemplate.header("Authorization", "Bearer " + this.getToken());
    }
}
