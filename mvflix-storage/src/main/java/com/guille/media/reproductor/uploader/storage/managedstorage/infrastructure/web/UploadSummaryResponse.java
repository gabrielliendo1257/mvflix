package com.guille.media.reproductor.uploader.storage.managedstorage.infrastructure.web;

import com.guille.media.reproductor.uploader.storage.managedstorage.domain.model.StoreObject.StorageSessionStatus;

import java.time.Instant;

public record UploadSummaryResponse(
    Long storageId,
    String objectKey,
    StorageSessionStatus status,
    long sizeInBytes,
    Instant createdAt) {}