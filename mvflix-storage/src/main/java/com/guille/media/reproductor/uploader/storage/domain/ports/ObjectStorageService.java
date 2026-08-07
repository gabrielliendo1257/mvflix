package com.guille.media.reproductor.uploader.storage.domain.ports;

import com.guille.media.reproductor.uploader.storage.domain.exceptions.StorageException;
import com.guille.media.reproductor.uploader.storage.domain.vos.*;

import reactor.core.publisher.Mono;

import java.util.List;

public interface ObjectStorageService {
    Mono<PermissionUrl> createUploadUrl(PresignedUploadRequest request, StorageLocation location);

    Mono<PermissionUrl> createStreamingUrl(PresignedUploadRequest request, StorageLocation location);

    Mono<Boolean> objectExists(StorageLocation location);

    Mono<StorageMetadata> getMetadata(StorageLocation location);

    Mono<Boolean> bucketExists(BucketName bucketName);

    void delete(String key);

    void copy(String sourceKey, String targetKey);

    void move(String sourceKey, String targetKey);

    List<StoredObjectSummary> list(String prefix);

    Mono<Void> createBucket(String nameBucket) throws StorageException;
}