package com.guille.media.reproductor.uploader.storage.managedstorage.application;

import java.time.Instant;
import java.util.UUID;

public sealed interface StorageIntegrationEvent<T>
    permits StoredObjectDeletedIntegrationEvent, UploadCompletedIntegrationEvent {
  UUID eventId();
  String eventType();
  int eventVersion();
  Instant occurredAt();
  String aggregateId();
  String aggregateType();
  T payload();
}
