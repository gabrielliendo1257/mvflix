package com.gcorp.service.app.mvflix_media_ingestion.domain;

import java.time.Instant;
import java.util.UUID;

public record MediaIngestion(
    UUID ingestionId,
    String actorId,
    Long catalogItemId,
    String uploadId,
    Phase phase,
    String failureCode,
    long version,
    int retryCount,
    Instant createdAt,
    Instant updatedAt,
    Instant nextAttemptAt,
    String idempotencyKey,
    String fileName,
    long fileSize,
    String mimeType,
    String uploadUrl,
    Long storageId,
    String storageKey) {
  public MediaIngestion(
      UUID id,
      String actor,
      Long catalog,
      String upload,
      Phase phase,
      String failure,
      long version,
      int retries,
      Instant created,
      Instant updated,
      Instant next,
      String key,
      String name,
      long size,
      String mime,
      String url) {
    this(
        id, actor, catalog, upload, phase, failure, version, retries, created, updated, next, key,
        name, size, mime, url, null, null);
  }

  public MediaIngestion(
      UUID id,
      String actor,
      Long catalog,
      String upload,
      Phase phase,
      String failure,
      long version,
      int retries,
      Instant created,
      Instant updated,
      Instant next,
      String key,
      String name,
      long size,
      String mime,
      String url,
      Long storageId) {
    this(
        id, actor, catalog, upload, phase, failure, version, retries, created, updated, next, key,
        name, size, mime, url, storageId, null);
  }

  public enum Phase {
    STARTING,
    PREPARING_CATALOG,
    PREPARING_UPLOAD,
    AWAITING_UPLOAD,
    FINALIZING_CATALOG,
    COMPLETED,
    CANCELLING,
    CANCELLED,
    FAILED,
    RECONCILIATION_REQUIRED
  }

  public MediaIngestion transition(Phase next, Long catalog, String upload, String failure) {
    if (phase == Phase.COMPLETED || phase == Phase.CANCELLED)
      throw new IllegalStateException("terminal ingestion");
    return new MediaIngestion(
        ingestionId,
        actorId,
        catalog == null ? catalogItemId : catalog,
        upload == null ? uploadId : upload,
        next,
        failure,
        version + 1,
        retryCount,
        createdAt,
        Instant.now(),
        nextAttemptAt,
        idempotencyKey,
        fileName,
        fileSize,
        mimeType,
        uploadUrl,
        storageId,
        storageKey);
  }

  public MediaIngestion failed(String code) {
    return recovery(Phase.FAILED, code, 30);
  }

  public MediaIngestion recovery(Phase next, String reason, long delaySeconds) {
    return new MediaIngestion(
        ingestionId,
        actorId,
        catalogItemId,
        uploadId,
        next,
        reason,
        version + 1,
        retryCount + 1,
        createdAt,
        Instant.now(),
        Instant.now().plusSeconds(delaySeconds),
        idempotencyKey,
        fileName,
        fileSize,
        mimeType,
        uploadUrl,
        storageId,
        storageKey);
  }

  public MediaIngestion rescheduled(Phase next, String reason, long delaySeconds) {
    return new MediaIngestion(
        ingestionId,
        actorId,
        catalogItemId,
        uploadId,
        next,
        reason,
        version + 1,
        retryCount,
        createdAt,
        Instant.now(),
        Instant.now().plusSeconds(delaySeconds),
        idempotencyKey,
        fileName,
        fileSize,
        mimeType,
        uploadUrl,
        storageId,
        storageKey);
  }
}
