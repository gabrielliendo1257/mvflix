package com.guille.media.reproductor.uploader.storage.managedstorage.application;

import java.time.Instant;
import java.util.UUID;

public record UploadCompletedIntegrationEvent(
    UUID eventId,
    int eventVersion,
    Instant occurredAt,
    String aggregateId,
    UploadCompletedPayload payload)
    implements StorageIntegrationEvent<UploadCompletedIntegrationEvent.UploadCompletedPayload> {

  @Override
  public String eventType() {
    return "UploadCompleted";
  }

  @Override
  public String aggregateType() {
    return "ManagedObject";
  }

  public record UploadCompletedPayload(
      Long storageId,
      String ownerUsername,
      String objectKey,
      String contentType,
      Long contentLength) {}
}
