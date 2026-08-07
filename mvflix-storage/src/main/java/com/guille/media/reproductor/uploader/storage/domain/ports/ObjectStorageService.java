package com.guille.media.reproductor.uploader.storage.domain.ports;

import com.guille.media.reproductor.uploader.storage.domain.vos.*;
import reactor.core.publisher.Mono;

import java.util.List;

public interface ObjectStorageService {
    Mono<PermissionUrl> createUploadUrl(PresignedUploadRequest request, StorageLocation location);

    Mono<PermissionUrl> createStreamingUrl(PresignedUploadRequest request, StorageLocation location);

    Mono<Boolean> objectExists(StorageLocation location);

    Mono<StorageMetadata> getMetadata(StorageLocation location);

    Mono<Boolean> bucketExists(BucketName bucketName);

    Mono<Void> delete(String key);

    Mono<Void> copy(String sourceKey, String targetKey);

    Mono<Void> move(String sourceKey, String targetKey);

    Mono<List<StoredObjectSummary>> list(String prefix);

    Mono<Void> createBucket(String nameBucket);
}
