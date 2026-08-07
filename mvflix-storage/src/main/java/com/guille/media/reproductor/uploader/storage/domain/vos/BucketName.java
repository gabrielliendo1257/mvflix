package com.guille.media.reproductor.uploader.storage.domain.vos;

public record BucketName(String bucketName) {
    public static BucketName of(String bucketName) {
        return new BucketName(bucketName);
    }
}
