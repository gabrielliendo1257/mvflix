package com.gcorp.service.app.mvflix_movies.catalog.application;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gcorp.service.app.mvflix_movies.catalog.domain.movie.MovieId;

import org.junit.jupiter.api.Test;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.UUID;

class StoredObjectDeletedConsumerTest {

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
    private final MovieDeletionTransaction deletionTransaction =
            org.mockito.Mockito.mock(MovieDeletionTransaction.class);
    private final StoredObjectDeletedConsumer consumer =
            new StoredObjectDeletedConsumer(objectMapper, deletionTransaction);

    @Test
    void finalizesTheReferencedManagedMovie() {
        UUID eventId = UUID.randomUUID();
        when(this.deletionTransaction.finalizeManagedDeletion(MovieId.of(42L), 7L))
                .thenReturn(Mono.empty());

        StepVerifier.create(this.consumer.consume(event(eventId)))
                .verifyComplete();

        verify(this.deletionTransaction).finalizeManagedDeletion(eq(MovieId.of(42L)), eq(7L));
    }

    @Test
    void rejectsAnEventWithAnUnsupportedType() {
        String event = event(UUID.randomUUID()).replace("StoredObjectDeleted", "OtherEvent");

        StepVerifier.create(this.consumer.consume(event))
                .expectError(IllegalArgumentException.class)
                .verify();
    }

    private static String event(UUID eventId) {
        return """
                {
                  "eventId":"%s",
                  "eventType":"StoredObjectDeleted",
                  "eventVersion":1,
                  "occurredAt":"2026-08-27T20:00:00Z",
                  "producer":"mvflix-storage",
                  "aggregate":{"type":"ManagedObject","id":"7"},
                  "payload":{"movieId":42,"storageId":7,"objectKey":"objects/7",
                    "ownerUsername":"pepe","releasedBytes":1024,"deletionStatus":"DELETED"}
                }
                """.formatted(eventId);
    }
}
