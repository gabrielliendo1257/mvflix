package com.guille.media.reproductor.uploader.storage.domain.ports;

import com.guille.media.reproductor.uploader.storage.domain.exceptions.StorageException;
import com.guille.media.reproductor.uploader.storage.domain.vos.*;

import java.util.List;

public interface ObjectStorageService {
    PermissionUrl createUploadUrl(PresignedUploadRequest request, StorageLocation location);

    PermissionUrl createStreamingUrl(PresignedUploadRequest request, StorageLocation location);

    boolean objectExists(StorageLocation location);

    StorageMetadata getMetadata(StorageLocation location);

    boolean bucketExists(BucketName bucketName);

    void delete(String key);

    void copy(String sourceKey, String targetKey);

    void move(String sourceKey, String targetKey);

    List<StoredObjectSummary> list(String prefix);

    void createBucket(String nameBucket) throws StorageException;
}
