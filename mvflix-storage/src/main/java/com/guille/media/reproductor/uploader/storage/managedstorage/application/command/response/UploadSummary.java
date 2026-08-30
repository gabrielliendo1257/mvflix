package com.guille.media.reproductor.uploader.storage.managedstorage.application.command.response;

import com.guille.media.reproductor.uploader.storage.managedstorage.domain.model.StorageObject.StorageSessionStatus;

import java.time.Instant;

/** Resumen de una sesión de subida para listados (sin URLs ni datos sensibles). */
public record UploadSummary(
    Long storageId,
    String objectKey,
    StorageSessionStatus status,
    long sizeInBytes,
    Instant createdAt) {}