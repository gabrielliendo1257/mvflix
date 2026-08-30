package com.gcorp.service.app.mvflix_media_ingestion.application;
import com.gcorp.service.app.mvflix_media_ingestion.domain.MediaIngestion;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

public interface Outbox {
  Mono<Void> started(MediaIngestion i); Mono<Void> completed(MediaIngestion i);
  Mono<Void> cancelled(MediaIngestion i); Mono<Void> failed(MediaIngestion i);
  Flux<Message> claim(int batchSize, int maxAttempts, Duration lease);
  Mono<Void> markPublished(UUID eventId);
  Mono<Void> markFailed(UUID eventId, String error, Duration retryDelay);
  Mono<Long> pendingCount(int maxAttempts);
  Mono<Long> exhaustedCount(int maxAttempts);
  Mono<Long> oldestPendingAgeSeconds();
  record Message(UUID eventId, String eventType, UUID aggregateId, String payload, Instant occurredAt) {}
}
