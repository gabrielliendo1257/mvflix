package com.guille.media.reproductor.uploader.storage.managedstorage.application;

import java.time.Instant;
import java.util.UUID;

public record StoredObjectDeletedIntegrationEvent(
    UUID eventId,
    int eventVersion,
    Instant occurredAt,
    String aggregateId,
    StoredObjectDeletedPayload payload)
    implements StorageIntegrationEvent<StoredObjectDeletedIntegrationEvent.StoredObjectDeletedPayload> {

  @Override
  public String eventType() {
    return "StoredObjectDeleted";
  }

  @Override
  public String aggregateType() {
    return "ManagedObject";
  }

  public record StoredObjectDeletedPayload(
      Long movieId,
      Long storageId,
      String objectKey,
      String ownerUsername,
      long releasedBytes,
      String deletionStatus) {}
}
