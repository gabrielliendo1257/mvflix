package com.guille.media.reproductor.uploader.storage.managedstorage.infrastructure.web;

import com.guille.media.reproductor.uploader.storage.managedstorage.application.RequestManagedObjectDeletion;

import io.swagger.v3.oas.annotations.tags.Tag;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import reactor.core.publisher.Mono;

/**
 * Borrado M2M de objetos MANAGED. Autorizado por el scope
 * {@code storage.objects.delete} (client_credentials de {@code movies-catalog}).
 *
 * <p>POST en lugar de DELETE con body: algunos proxies/clientes manejan mal
 * el body en DELETE. Idempotente: repetir el borrado responde 204.
 */
@Tag(name = "Managed Objects", description = "Borrado M2M de objetos MANAGED (movies-catalog)")
@RestController
@RequestMapping(value = "/api/v1/movie/storage/objects", produces = MediaType.APPLICATION_JSON_VALUE)
public class ManagedObjectDeletionController {

    private final RequestManagedObjectDeletion requestDeletion;

    public ManagedObjectDeletionController(RequestManagedObjectDeletion requestDeletion) {
        this.requestDeletion = requestDeletion;
    }

    @PostMapping(value = "/{storageId}/deletion", consumes = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ResponseEntity<Void>> delete(
            @PathVariable Long storageId,
            @RequestBody ManagedObjectDeletionRequest request) {
        return this.requestDeletion
                .execute(storageId, request.expectedOwner(), request.expectedObjectKey())
                .thenReturn(ResponseEntity.noContent().build());
    }
}
