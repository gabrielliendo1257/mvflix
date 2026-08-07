package com.guille.media.reproductor.uploader.storage.domain.models;

import com.guille.media.reproductor.uploader.storage.domain.exceptions.ExceededQuotaException;
import com.guille.media.reproductor.uploader.storage.domain.exceptions.IllegalConsumeBytes;

import com.guille.media.reproductor.uploader.storage.domain.vos.BucketName;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
@AllArgsConstructor
public class UserStorage {
    private final Long id;
    private final BucketName bucketName;
    private final String ownerUsername;
    private StorageQuota storageQuota;
    private StorageUsage storageUsage;

    public void consumeStorage(long bytes) {
        if (bytes <= 0) {
            throw new IllegalConsumeBytes("Los bytes deben ser mayores que cero");
        }
        StorageUsage nextUsage = this.storageUsage.addBytes(bytes);

        if (this.storageQuota.isExceeded(nextUsage)) {
            throw new ExceededQuotaException("Excede el storage permitido.");
        }

        this.storageUsage = nextUsage;
    }
}
