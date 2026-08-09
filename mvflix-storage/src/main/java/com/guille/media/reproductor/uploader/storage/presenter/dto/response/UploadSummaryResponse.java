package com.guille.media.reproductor.uploader.storage.presenter.dto.response;

import com.guille.media.reproductor.uploader.storage.domain.models.StoreObject.StorageSessionStatus;

import java.time.Instant;

public record UploadSummaryResponse(
    Long storageId,
    String objectKey,
    StorageSessionStatus status,
    long sizeInBytes,
    Instant createdAt) {}