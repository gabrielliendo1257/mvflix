package com.gcorp.service.app.mvflix_movies.catalog.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.gcorp.service.app.mvflix_movies.catalog.application.port.OutboxRepository;
import com.gcorp.service.app.mvflix_movies.support.PostgresIntegrationTest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.test.context.ActiveProfiles;

import reactor.test.StepVerifier;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

@ActiveProfiles("sandbox")
@SpringBootTest
class OutboxRepositoryIntegrationTest extends PostgresIntegrationTest {

    @Autowired private OutboxRepository outboxRepository;
    @Autowired private DatabaseClient databaseClient;

    @BeforeEach
    void cleanDatabase() {
        this.databaseClient.sql("DELETE FROM outbox_events").fetch().rowsUpdated().block();
    }

    @Test
    void claimLocksPendingRowsAndPublishedRowsAreNotClaimedAgain() {
        UUID eventId = insertEvent();

        StepVerifier.create(this.outboxRepository.claim(25, 10, Duration.ofMinutes(2)))
                .assertNext(message -> {
                    assertThat(message.eventId()).isEqualTo(eventId);
                    assertThat(message.eventType()).isEqualTo("ManagedMediaDeletionRequested");
                })
                .verifyComplete();

        StepVerifier.create(this.outboxRepository.claim(25, 10, Duration.ofMinutes(2)))
                .verifyComplete();

        StepVerifier.create(this.outboxRepository.markPublished(eventId)).verifyComplete();
        StepVerifier.create(this.outboxRepository.pendingCount(10)).expectNext(0L).verifyComplete();
    }

    @Test
    void failedRowReceivesRetryMetadata() {
        UUID eventId = insertEvent();

        this.outboxRepository.claim(25, 10, Duration.ofMinutes(2)).collectList().block();
        StepVerifier.create(this.outboxRepository.markFailed(
                        eventId, "Kafka unavailable", Duration.ofMinutes(1)))
                .verifyComplete();

        StepVerifier.create(this.databaseClient
                        .sql("SELECT attempts, locked_until, last_error FROM outbox_events WHERE event_id = :id")
                        .bind("id", eventId)
                        .map((row, metadata) -> java.util.List.of(
                                String.valueOf(row.get("attempts", Integer.class)),
                                String.valueOf(row.get("locked_until", Instant.class)),
                                row.get("last_error", String.class)))
                        .one())
                .assertNext(row -> {
                    assertThat(row.get(0)).isEqualTo("1");
                    assertThat(row.get(1)).isEqualTo("null");
                    assertThat(row.get(2)).isEqualTo("Kafka unavailable");
                })
                .verifyComplete();
    }

    private UUID insertEvent() {
        UUID eventId = UUID.randomUUID();
        this.databaseClient
                .sql("""
                        INSERT INTO outbox_events (
                            event_id, event_type, event_version, aggregate_type,
                            aggregate_id, occurred_at, payload)
                        VALUES (:id, 'ManagedMediaDeletionRequested', 1, 'Movie',
                                '7', NOW(), '{"payload":{}}'::jsonb)
                        """)
                .bind("id", eventId)
                .fetch()
                .rowsUpdated()
                .block();
        return eventId;
    }
}
