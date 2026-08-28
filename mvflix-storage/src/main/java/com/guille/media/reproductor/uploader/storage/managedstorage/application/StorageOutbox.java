package com.guille.media.reproductor.uploader.storage.managedstorage.application;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

public interface StorageOutbox {
  Mono<Void> append(StorageIntegrationEvent event);

  Mono<Long> purgePublishedBefore(Instant cutoff);

  Flux<StorageOutboxMessage> claim(int batchSize, int maxAttempts, Duration lease);

  Mono<Void> markPublished(UUID eventId);

  Mono<Void> markFailed(UUID eventId, String error, Duration retryDelay);

  Mono<Long> pendingCount(int maxAttempts);

  Mono<Long> exhaustedCount(int maxAttempts);

  Mono<Long> oldestPendingAgeSeconds();

  Mono<Void> reactivateExhausted(int maxAttempts);
}
