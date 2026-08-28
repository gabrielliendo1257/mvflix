package com.guille.media.reproductor.uploader.storage.managedstorage.application;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.UUID;

public interface StoredObjectDeletedOutbox {
  Mono<Void> append(ManagedMediaDeletionRequested event, DeleteStoredObject.DeletionResult result);

  Flux<StorageOutboxMessage> claim(int batchSize, int maxAttempts, Duration lease);

  Mono<Void> markPublished(UUID eventId);

  Mono<Void> markFailed(UUID eventId, String error, Duration retryDelay);

  Mono<Long> pendingCount(int maxAttempts);
}
