package com.gcorp.service.app.mvflix_movies.catalog.infrastructure.client.storage;

import com.gcorp.service.app.mvflix_movies.catalog.application.port.ManagedObjectDeletion;
import com.gcorp.service.app.mvflix_movies.catalog.application.port.ManagedObjectDeletionInconsistentException;
import com.gcorp.service.app.mvflix_movies.catalog.application.port.ManagedObjectDeletionUnavailableException;
import com.gcorp.service.app.mvflix_movies.catalog.application.port.ManagedObjectReference;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import reactor.core.publisher.Mono;

/**
 * Adapter del puerto {@link ManagedObjectDeletion} contra el endpoint M2M de
 * Storage. El WebClient y el token client_credentials viven aquí; el dominio
 * solo ve {@code Mono<Void>} y errores de aplicación.
 *
 * <p>Idempotencia: un 404 (objeto ya ausente) se traduce a éxito. Un 409
 * (owner/objectKey no coinciden) o un 401/403 (scope/secret) son
 * {@code ManagedObjectDeletionInconsistentException}; la caída de Storage es
 * {@code ManagedObjectDeletionUnavailableException}.
 */
@Slf4j
@Component
@Profile("!sandbox")
public class StorageManagedObjectDeletionAdapter implements ManagedObjectDeletion {

    private static final String DELETION_PATH = "/api/v1/movie/storage/objects/{storageId}/deletion";

    private final WebClient webClient;

    public StorageManagedObjectDeletionAdapter(
            WebClient.Builder builder,
            ManagedObjectDeletionTokenProvider tokenProvider,
            @Value("${services.storage.url}") String storageUrl) {
        ExchangeFilterFunction bearer =
                ExchangeFilterFunction.ofRequestProcessor(request ->
                        tokenProvider.token().map(token -> ClientRequest.from(request)
                                .headers(headers -> headers.setBearerAuth(token))
                                .build()));
        this.webClient = builder.baseUrl(storageUrl).filter(bearer).build();
    }

    @Override
    public Mono<Void> delete(ManagedObjectReference reference) {
        return this.webClient
                .post()
                .uri(DELETION_PATH, reference.storageId())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new DeletionRequest(reference.ownerUsername(), reference.objectKey()))
                .retrieve()
                .toBodilessEntity()
                .then()
                .onErrorResume(WebClientResponseException.class,
                        error -> translateResponse(reference.storageId(), error))
                .onErrorResume(WebClientRequestException.class, error -> {
                    log.warn("Storage no alcanzable para borrar storageId={}: {}",
                            reference.storageId(), error.getMessage());
                    return Mono.error(new ManagedObjectDeletionUnavailableException(
                            "Storage no alcanzable", error));
                });
    }

    private Mono<Void> translateResponse(long storageId, WebClientResponseException error) {
        int status = error.getStatusCode().value();
        if (status == 404) {
            log.info("Objeto de storage ya ausente (idempotente): storageId={}", storageId);
            return Mono.empty();
        }
        if (status == 409) {
            log.warn("Asociación corrupta al borrar storageId={}: owner/objectKey no coinciden", storageId);
            return Mono.error(new ManagedObjectDeletionInconsistentException(
                    "owner/objectKey mismatch for storageId=" + storageId));
        }
        if (status == 401 || status == 403) {
            log.warn("Cliente M2M rechazado al borrar storageId={}: status={}", storageId, status);
            return Mono.error(new ManagedObjectDeletionInconsistentException(
                    "storage.objects.delete scope missing or secret invalid (status=" + status + ")"));
        }
        log.warn("Storage respondió {} al borrar storageId={}", status, storageId);
        return Mono.error(new ManagedObjectDeletionUnavailableException(
                "Storage respondió " + status, error));
    }

    record DeletionRequest(String expectedOwner, String expectedObjectKey) {}
}
