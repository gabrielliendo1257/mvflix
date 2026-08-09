package com.guille.media.reproductor.uploader.storage.app.commands.response;

import com.guille.media.reproductor.uploader.storage.domain.models.StoreObject.StorageSessionStatus;

import java.time.Instant;

/** Resumen de una sesión de subida para listados (sin URLs ni datos sensibles). */
public record UploadSummary(
    Long storageId,
    String objectKey,
    StorageSessionStatus status,
    long sizeInBytes,
    Instant createdAt) {}