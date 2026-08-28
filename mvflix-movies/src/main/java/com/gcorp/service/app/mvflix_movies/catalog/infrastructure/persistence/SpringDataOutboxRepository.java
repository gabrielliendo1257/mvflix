package com.gcorp.service.app.mvflix_movies.catalog.infrastructure.persistence;

import com.gcorp.service.app.mvflix_movies.catalog.application.port.OutboxMessage;
import com.gcorp.service.app.mvflix_movies.catalog.application.port.OutboxRepository;

import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.UUID;

/**
 * Claim transaccional de outbox. {@code SKIP LOCKED} permite varios pollers sin
 * que esperen entre sí; el lease evita que una caída deje filas bloqueadas.
 */
@Repository
public class SpringDataOutboxRepository implements OutboxRepository {

    private final DatabaseClient databaseClient;

    public SpringDataOutboxRepository(DatabaseClient databaseClient) {
        this.databaseClient = databaseClient;
    }

    @Override
    @Transactional(transactionManager = "connectionFactoryTransactionManager")
    public Flux<OutboxMessage> claim(int batchSize, int maxAttempts, Duration lease) {
        return this.databaseClient
                .sql(
                        """
                        WITH candidates AS (
                            SELECT event_id
                            FROM outbox_events
                            WHERE published_at IS NULL
                              AND attempts < :max_attempts
                              AND next_attempt_at <= NOW()
                              AND (locked_until IS NULL OR locked_until < NOW())
                            ORDER BY created_at, event_id
                            LIMIT :batch_size
                            FOR UPDATE SKIP LOCKED
                        )
                        UPDATE outbox_events e
                        SET attempts = e.attempts + 1,
                            locked_until = NOW() + make_interval(secs => :lease_seconds)
                        FROM candidates c
                        WHERE e.event_id = c.event_id
                        RETURNING e.event_id, e.event_type, e.event_version,
                                  e.aggregate_id, e.payload::text
                        """)
                .bind("batch_size", batchSize)
                .bind("max_attempts", maxAttempts)
                .bind("lease_seconds", Math.max(1L, lease.toSeconds()))
                .map((row, metadata) -> new OutboxMessage(
                        row.get("event_id", UUID.class),
                        row.get("event_type", String.class),
                        row.get("event_version", Integer.class),
                        row.get("aggregate_id", String.class),
                        row.get("payload", String.class)))
                .all();
    }

    @Override
    public Mono<Void> markPublished(UUID eventId) {
        return this.databaseClient
                .sql("UPDATE outbox_events SET published_at = NOW(), locked_until = NULL "
                        + "WHERE event_id = :event_id AND published_at IS NULL")
                .bind("event_id", eventId)
                .fetch()
                .rowsUpdated()
                .then();
    }

    @Override
    public Mono<Void> markFailed(UUID eventId, String error, Duration retryDelay) {
        return this.databaseClient
                .sql(
                        """
                        UPDATE outbox_events
                        SET locked_until = NULL,
                            next_attempt_at = NOW() + make_interval(secs => :retry_seconds),
                            last_error = :last_error
                        WHERE event_id = :event_id AND published_at IS NULL
                        """)
                .bind("retry_seconds", Math.max(1L, retryDelay.toSeconds()))
                .bind("last_error", error == null ? "unknown publish error" : error.substring(0, Math.min(2000, error.length())))
                .bind("event_id", eventId)
                .fetch()
                .rowsUpdated()
                .then();
    }

    @Override
    public Mono<Long> pendingCount(int maxAttempts) {
        return this.databaseClient
                .sql("SELECT COUNT(*) AS n FROM outbox_events "
                        + "WHERE published_at IS NULL AND attempts < :max_attempts")
                .bind("max_attempts", maxAttempts)
                .map((row, metadata) -> row.get("n", Long.class))
                .one();
    }
}
