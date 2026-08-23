package com.guille.media.reproductor.uploader.storage.domain.ports;

import com.guille.media.reproductor.uploader.storage.domain.vos.*;

import reactor.core.publisher.Mono;

public interface ObjectStorageService {
    Mono<PermissionUrl> createUploadUrl(PresignedUploadRequest request, StorageLocation location);

    Mono<PermissionUrl> createStreamingUrl(PresignedUploadRequest request, StorageLocation location);

    Mono<Boolean> objectExists(StorageLocation location);

    Mono<StorageMetadata> getMetadata(StorageLocation location);

    Mono<Boolean> bucketExists(BucketName bucketName);

    void delete(StorageLocation location);

    Mono<Void> createBucket(String nameBucket);

    /**
     * Crea el bucket si no existe (idempotente).
     */
    Mono<Void> ensureBucket(BucketName bucketName);

    /**
     * Crea la estructura de carpetas del usuario ({@code <username>/images/}, ...)
     * dentro del bucket si no existe (idempotente).
     */
    Mono<Void> ensureUserStorageLayout(BucketName bucketName, String username);
}
