package com.gcorp.service.app.mvflix_movies.catalog.infrastructure.persistence;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gcorp.service.app.mvflix_movies.catalog.application.port.ManagedDeletionOutbox;
import com.gcorp.service.app.mvflix_movies.catalog.application.port.ManagedMediaDeletionRequested;

import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Repository;

import reactor.core.publisher.Mono;

import java.util.Map;

/** Adapter R2DBC de la outbox; la transacción la delimita el Application Service. */
@Repository
public class SpringDataManagedDeletionOutbox implements ManagedDeletionOutbox {

    private final DatabaseClient databaseClient;
    private final ObjectMapper objectMapper;

    public SpringDataManagedDeletionOutbox(DatabaseClient databaseClient, ObjectMapper objectMapper) {
        this.databaseClient = databaseClient;
        this.objectMapper = objectMapper;
    }

    @Override
    public Mono<Void> append(ManagedMediaDeletionRequested event) {
        final String payload;
        try {
            Map<String, Object> eventPayload = Map.of(
                    "movieId", event.movieId(),
                    "storageId", event.storageId(),
                    "ownerUsername", event.ownerUsername(),
                    "objectKey", event.objectKey());
            payload = this.objectMapper.writeValueAsString(Map.of(
                    "eventId", event.eventId(),
                    "eventType", "ManagedMediaDeletionRequested",
                    "eventVersion", 1,
                    "occurredAt", event.occurredAt(),
                    "producer", "mvflix-movies",
                    "aggregate", Map.of("type", "Movie", "id", String.valueOf(event.movieId())),
                    "payload", eventPayload));
        } catch (JsonProcessingException error) {
            return Mono.error(new IllegalArgumentException("Cannot encode deletion event", error));
        }

        return this.databaseClient
                .sql(
                        """
                        INSERT INTO outbox_events (
                            event_id, event_type, event_version, aggregate_type,
                            aggregate_id, occurred_at, payload)
                        VALUES (:event_id, :event_type, :event_version, :aggregate_type,
                                :aggregate_id, :occurred_at, CAST(:payload AS jsonb))
                        ON CONFLICT (event_type, aggregate_id)
                        WHERE event_type = 'ManagedMediaDeletionRequested' DO NOTHING
                        """)
                .bind("event_id", event.eventId())
                .bind("event_type", "ManagedMediaDeletionRequested")
                .bind("event_version", 1)
                .bind("aggregate_type", "Movie")
                .bind("aggregate_id", String.valueOf(event.movieId()))
                .bind("occurred_at", event.occurredAt())
                .bind("payload", payload)
                .fetch()
                .rowsUpdated()
                .then();
    }

    @Override
    public Mono<Void> reactivateExhausted(String movieId, int maxAttempts) {
        return this.databaseClient
                .sql("""
                        UPDATE outbox_events
                        SET attempts = 0, next_attempt_at = NOW(), locked_until = NULL, last_error = NULL
                        WHERE event_type = 'ManagedMediaDeletionRequested'
                          AND aggregate_id = :aggregate_id
                          AND published_at IS NULL
                          AND attempts >= :max_attempts
                        """)
                .bind("aggregate_id", movieId)
                .bind("max_attempts", maxAttempts)
                .fetch()
                .rowsUpdated()
                .then();
    }
}
