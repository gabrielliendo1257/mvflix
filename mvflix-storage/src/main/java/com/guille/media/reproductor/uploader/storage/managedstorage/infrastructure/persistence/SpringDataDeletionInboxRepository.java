package com.guille.media.reproductor.uploader.storage.managedstorage.infrastructure.persistence;

import com.guille.media.reproductor.uploader.storage.managedstorage.domain.port.DeletionInboxRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Repository;

import reactor.core.publisher.Mono;

import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class SpringDataDeletionInboxRepository implements DeletionInboxRepository {

  private final DatabaseClient databaseClient;

  @Override
  public Mono<Boolean> isCompleted(UUID eventId) {
    return this.databaseClient
        .sql("SELECT status FROM managed_media_deletion_inbox WHERE event_id = :eventId")
        .bind("eventId", eventId)
        .map((row, metadata) -> "COMPLETED".equals(row.get("status", String.class)))
        .one()
        .defaultIfEmpty(false);
  }

  @Override
  public Mono<Void> recordReceived(UUID eventId) {
    return this.databaseClient
        .sql("""
            INSERT INTO managed_media_deletion_inbox (event_id, status)
            VALUES (:eventId, 'RECEIVED')
            ON CONFLICT (event_id) DO NOTHING
            """)
        .bind("eventId", eventId)
        .fetch()
        .rowsUpdated()
        .then();
  }

  @Override
  public Mono<Void> markCompleted(UUID eventId) {
    return this.databaseClient
        .sql("""
            UPDATE managed_media_deletion_inbox
            SET status = 'COMPLETED', completed_at = NOW(), last_error = NULL
            WHERE event_id = :eventId
            """)
        .bind("eventId", eventId)
        .fetch()
        .rowsUpdated()
        .then();
  }

  @Override
  public Mono<Void> markFailed(UUID eventId, String error) {
    return this.databaseClient
        .sql("""
            UPDATE managed_media_deletion_inbox
            SET status = 'RECEIVED', last_error = :error
            WHERE event_id = :eventId
            """)
        .bind("eventId", eventId)
        .bind("error", error)
        .fetch()
        .rowsUpdated()
        .then();
  }
}
