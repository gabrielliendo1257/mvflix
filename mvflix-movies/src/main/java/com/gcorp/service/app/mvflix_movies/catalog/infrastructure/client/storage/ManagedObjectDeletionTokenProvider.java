package com.gcorp.service.app.mvflix_movies.catalog.infrastructure.client.storage;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

import reactor.core.publisher.Mono;

import java.time.Instant;

/**
 * Token client_credentials del machine-client {@code movies-catalog}
 * (scope {@code storage.objects.delete}), cacheado hasta 60s antes de expirar.
 * Espejo del patrón de playback en el BFF.
 */
@Slf4j
@Component
@Profile("!sandbox")
public class ManagedObjectDeletionTokenProvider {

    private final WebClient authWebClient;
    private final String clientId;
    private final String clientSecret;
    private final String scope;

    private volatile String cachedToken;
    private volatile Instant expiresAt = Instant.EPOCH;

    public ManagedObjectDeletionTokenProvider(
            @Value("${services.authorization.url}") String authorizationUrl,
            @Value("${movies-catalog.client-id:movies-catalog}") String clientId,
            @Value("${MOVIES_CATALOG_SECRET:super-secret}") String clientSecret,
            @Value("${storage.objects.delete.scope:storage.objects.delete}") String scope,
            WebClient.Builder builder) {
        this.authWebClient = builder.baseUrl(authorizationUrl).build();
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.scope = scope;
    }

    public synchronized Mono<String> token() {
        if (this.cachedToken != null && Instant.now().isBefore(this.expiresAt)) {
            return Mono.just(this.cachedToken);
        }
        return this.authWebClient
                .post()
                .uri("/oauth2/token")
                .headers(h -> h.setBasicAuth(this.clientId, this.clientSecret))
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(BodyInserters.fromFormData("grant_type", "client_credentials")
                        .with("scope", this.scope))
                .retrieve()
                .bodyToMono(TokenResponse.class)
                .doOnNext(response -> {
                    this.cachedToken = response.accessToken();
                    this.expiresAt = Instant.now().plusSeconds(Math.max(60, response.expiresIn() - 60));
                    log.info("Storage deletion token renovado (expira en {}s)", response.expiresIn());
                })
                .map(TokenResponse::accessToken);
    }

    record TokenResponse(
            @JsonProperty("access_token") String accessToken,
            @JsonProperty("expires_in") long expiresIn,
            @JsonProperty("token_type") String tokenType) {}
}
