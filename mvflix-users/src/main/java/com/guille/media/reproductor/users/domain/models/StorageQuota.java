package com.guille.media.reproductor.users.domain.models;

import lombok.Getter;
import lombok.ToString;

/**
 * Representa los bytes que se le asignan al usuario según su {@link Plan}.
 */
@Getter
@ToString
public class StorageQuota {
    private final long userBytesQuota;

    private static final long KB = 1024L;
    private static final long MB = KB * 1024L;
    private static final long GB = MB * 1024L;

    private static final long ENTERPRISE_MAX_UPLOAD_SIZE = 1024L * GB;
    private static final long PRO_MAX_UPLOAD_SIZE = 100L * GB;
    private static final long FREE_MAX_UPLOAD_SIZE = 500L * MB;

    public StorageQuota(long userBytesQuota) {
        this.userBytesQuota = userBytesQuota;
    }

    public boolean isExceeded(long usedBytes) {
        return userBytesQuota < usedBytes;
    }

    public long remaining(long usedBytes) {
        return Math.max(0, userBytesQuota - usedBytes);
    }

    public static StorageQuota getQuota(Plan plan) {
        return switch (plan) {
            case FREE -> new StorageQuota(FREE_MAX_UPLOAD_SIZE);
            case PRO -> new StorageQuota(PRO_MAX_UPLOAD_SIZE);
            case ENTERPRISE -> new StorageQuota(ENTERPRISE_MAX_UPLOAD_SIZE);
        };
    }
}