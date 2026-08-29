package com.guille.media.reproductor.uploader.storage.managedstorage.application;

import com.guille.media.reproductor.uploader.storage.managedstorage.domain.model.StoreObject;
import com.guille.media.reproductor.uploader.storage.managedstorage.domain.model.StoreObject.StorageSessionStatus;
import com.guille.media.reproductor.uploader.storage.managedstorage.domain.port.StorageRepository;

import org.springframework.stereotype.Component;
import org.springframework.transaction.reactive.TransactionalOperator;

import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Component
public class UploadCompletionTransaction {
  private final StorageRepository storageRepository;
  private final StorageOutbox storageOutbox;
  private final TransactionalOperator transactionalOperator;

  public UploadCompletionTransaction(
      StorageRepository storageRepository,
      StorageOutbox storageOutbox,
      TransactionalOperator transactionalOperator) {
    this.storageRepository = storageRepository;
    this.storageOutbox = storageOutbox;
    this.transactionalOperator = transactionalOperator;
  }

  public Mono<StoreObject> complete(StoreObject object) {
    return this.transactionalOperator.transactional(
        this.storageRepository.updateStatus(object, StorageSessionStatus.PENDING)
            .flatMap(saved -> this.storageOutbox.append(eventFor(saved)).thenReturn(saved)));
  }

  private StorageIntegrationEvent eventFor(StoreObject object) {
    UUID eventId = UUID.nameUUIDFromBytes(
        ("UploadCompleted:" + object.getStorageId()).getBytes(StandardCharsets.UTF_8));
    return new StorageIntegrationEvent(
        eventId,
        "UploadCompleted",
        1,
        Instant.now(),
        String.valueOf(object.getStorageId()),
        Map.of(
            "storageId", object.getStorageId(),
            "ownerUsername", object.getOwnerUsername(),
            "objectKey", object.getStorageKey().key(),
            "contentType", object.contentType(),
            "contentLength", object.sizeInBytes()));
  }
}
