package com.gcorp.service.app.mvflix_movies.catalog.application.port;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.UUID;

/** Puerto de entrega de la outbox; no expone SQL al Application Service. */
public interface OutboxRepository {

    Flux<OutboxMessage> claim(int batchSize, int maxAttempts, Duration lease);

    Mono<Void> markPublished(UUID eventId);

    Mono<Void> markFailed(UUID eventId, String error, Duration retryDelay);

    Mono<Long> pendingCount(int maxAttempts);

    Mono<Long> exhaustedCount(int maxAttempts);

    Mono<Long> oldestPendingAgeSeconds();
}
