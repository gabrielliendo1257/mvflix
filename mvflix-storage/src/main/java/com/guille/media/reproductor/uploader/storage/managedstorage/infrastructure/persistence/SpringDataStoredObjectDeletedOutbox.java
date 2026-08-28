package com.guille.media.reproductor.uploader.storage.managedstorage.infrastructure.persistence;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.guille.media.reproductor.uploader.storage.managedstorage.application.DeleteStoredObject;
import com.guille.media.reproductor.uploader.storage.managedstorage.application.ManagedMediaDeletionRequested;
import com.guille.media.reproductor.uploader.storage.managedstorage.application.StorageOutboxMessage;
import com.guille.media.reproductor.uploader.storage.managedstorage.application.StoredObjectDeletedOutbox;

import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Repository
public class SpringDataStoredObjectDeletedOutbox implements StoredObjectDeletedOutbox {

  private final DatabaseClient databaseClient;
  private final ObjectMapper objectMapper;

  public SpringDataStoredObjectDeletedOutbox(DatabaseClient databaseClient, ObjectMapper objectMapper) {
    this.databaseClient = databaseClient;
    this.objectMapper = objectMapper;
  }

  @Override
  public Mono<Void> append(ManagedMediaDeletionRequested event, DeleteStoredObject.DeletionResult result) {
    UUID outputEventId = UUID.nameUUIDFromBytes(event.eventId().toString().getBytes(java.nio.charset.StandardCharsets.UTF_8));
    String payload;
    try {
      payload = this.objectMapper.writeValueAsString(Map.of(
          "eventId", outputEventId,
          "eventType", "StoredObjectDeleted",
          "eventVersion", 1,
          "occurredAt", Instant.now(),
          "producer", "mvflix-storage",
          "aggregate", Map.of("type", "ManagedObject", "id", String.valueOf(event.storageId())),
          "payload", Map.of(
              "movieId", event.movieId(),
              "storageId", event.storageId(),
              "objectKey", event.objectKey(),
              "ownerUsername", event.ownerUsername(),
              "releasedBytes", result.releasedBytes(),
              "deletionStatus", result.deletionStatus())));
    } catch (JsonProcessingException error) {
      return Mono.error(new IllegalArgumentException("Cannot encode deletion confirmation", error));
    }
    return this.databaseClient.sql("""
        INSERT INTO storage_outbox_events
            (event_id, event_type, event_version, aggregate_id, occurred_at, payload)
        VALUES (:eventId, 'StoredObjectDeleted', 1, :aggregateId, :occurredAt, CAST(:payload AS jsonb))
        ON CONFLICT (event_id) DO NOTHING
        """)
        .bind("eventId", outputEventId)
        .bind("aggregateId", String.valueOf(event.storageId()))
        .bind("occurredAt", Instant.now())
        .bind("payload", payload)
        .fetch().rowsUpdated().then();
  }

  @Override
  @Transactional(transactionManager = "connectionFactoryTransactionManager")
  public Flux<StorageOutboxMessage> claim(int batchSize, int maxAttempts, Duration lease) {
    return this.databaseClient.sql("""
        WITH candidates AS (
          SELECT event_id FROM storage_outbox_events
          WHERE published_at IS NULL AND attempts < :maxAttempts
            AND next_attempt_at <= NOW()
            AND (locked_until IS NULL OR locked_until < NOW())
          ORDER BY created_at, event_id LIMIT :batchSize FOR UPDATE SKIP LOCKED
        )
        UPDATE storage_outbox_events e
        SET attempts = e.attempts + 1,
            locked_until = NOW() + make_interval(secs => :leaseSeconds)
        FROM candidates c WHERE e.event_id = c.event_id
        RETURNING e.event_id, e.event_type, e.event_version, e.aggregate_id, e.payload::text
        """)
        .bind("batchSize", batchSize)
        .bind("maxAttempts", maxAttempts)
        .bind("leaseSeconds", Math.max(1L, lease.toSeconds()))
        .map((row, metadata) -> new StorageOutboxMessage(
            row.get("event_id", UUID.class), row.get("event_type", String.class),
            row.get("event_version", Integer.class), row.get("aggregate_id", String.class),
            row.get("payload", String.class)))
        .all();
  }

  @Override
  public Mono<Void> markPublished(UUID eventId) {
    return this.databaseClient.sql("UPDATE storage_outbox_events SET published_at = NOW(), locked_until = NULL WHERE event_id = :eventId AND published_at IS NULL")
        .bind("eventId", eventId).fetch().rowsUpdated().then();
  }

  @Override
  public Mono<Void> markFailed(UUID eventId, String error, Duration retryDelay) {
    return this.databaseClient.sql("UPDATE storage_outbox_events SET locked_until = NULL, next_attempt_at = NOW() + make_interval(secs => :retrySeconds), last_error = :lastError WHERE event_id = :eventId AND published_at IS NULL")
        .bind("retrySeconds", Math.max(1L, retryDelay.toSeconds()))
        .bind("lastError", error == null ? "unknown publish error" : error.substring(0, Math.min(2000, error.length())))
        .bind("eventId", eventId).fetch().rowsUpdated().then();
  }

  @Override
  public Mono<Long> pendingCount(int maxAttempts) {
    return this.databaseClient.sql("SELECT COUNT(*) AS n FROM storage_outbox_events WHERE published_at IS NULL AND attempts < :maxAttempts")
        .bind("maxAttempts", maxAttempts).map((row, metadata) -> row.get("n", Long.class)).one();
  }

  @Override
  public Mono<Long> exhaustedCount(int maxAttempts) {
    return this.databaseClient.sql("SELECT COUNT(*) AS n FROM storage_outbox_events "
            + "WHERE published_at IS NULL AND attempts >= :maxAttempts")
        .bind("maxAttempts", maxAttempts)
        .map((row, metadata) -> row.get("n", Long.class)).one();
  }

  @Override
  public Mono<Long> oldestPendingAgeSeconds() {
    return this.databaseClient.sql("SELECT COALESCE(EXTRACT(EPOCH FROM (NOW() - MIN(created_at))), 0)::bigint AS age "
            + "FROM storage_outbox_events WHERE published_at IS NULL")
        .map((row, metadata) -> row.get("age", Long.class)).one();
  }

  @Override
  public Mono<Void> reactivateExhausted(int maxAttempts) {
    return this.databaseClient.sql("UPDATE storage_outbox_events "
            + "SET attempts = 0, next_attempt_at = NOW(), locked_until = NULL, last_error = NULL "
            + "WHERE published_at IS NULL AND attempts >= :maxAttempts")
        .bind("maxAttempts", maxAttempts).fetch().rowsUpdated().then();
  }
}
