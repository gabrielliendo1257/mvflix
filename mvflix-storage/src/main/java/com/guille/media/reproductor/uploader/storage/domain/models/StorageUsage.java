package com.guille.media.reproductor.uploader.storage.domain.models;

import lombok.Getter;

@Getter
public class StorageUsage {
    private long currentBytesUsage;

    public StorageUsage addBytes(long bytes) {
        this.currentBytesUsage = this.currentBytesUsage + bytes;
        return this;
    }

    public StorageUsage dismissBytes(long bytes) {
        this.currentBytesUsage = this.currentBytesUsage - bytes;
        return this;
    }

    public long remaining(StorageQuota storageQuota) {
        return storageQuota.getUserBytesQuota() - this.currentBytesUsage;
    }
}
