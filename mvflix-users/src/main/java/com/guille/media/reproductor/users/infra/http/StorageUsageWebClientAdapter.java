package com.guille.media.reproductor.users.infra.http;

import com.guille.media.reproductor.users.domain.ports.StorageUsagePort;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import reactor.core.publisher.Mono;

/**
 * Adaptador reactivo de {@link StorageUsagePort} hacia el storage-service
 * (contrato M2M: GET /storage/users/{username}/quota con scope {@code storage.read}).
 */
@Slf4j
@Component
@Profile("!sandbox")
@RequiredArgsConstructor
public class StorageUsageWebClientAdapter implements StorageUsagePort {

    private final WebClient storageServiceWebClient;

    @Override
    public Mono<Long> usedBytesBy(String username) {
        return this.storageServiceWebClient
                .get()
                .uri("/api/v1/movie/storage/users/{username}/quota", username)
                .retrieve()
                .bodyToMono(QuotaResponse.class)
                .map(QuotaResponse::usedBytes)
                .doOnNext(
                        used -> log.debug("Uso real obtenido: username={} usedBytes={}", username, used))
                .onErrorMap(
                        err ->
                                new IllegalStateException(
                                        "No se pudo consultar el uso real de " + username, err));
    }

    record QuotaResponse(String ownerUsername, String bucketName, long quotaBytes, long usedBytes,
            long remainingBytes) {}
}