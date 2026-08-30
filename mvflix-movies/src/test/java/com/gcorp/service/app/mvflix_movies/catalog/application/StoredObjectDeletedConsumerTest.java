package com.gcorp.service.app.mvflix_movies.catalog.application;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.never;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gcorp.service.app.mvflix_movies.catalog.domain.movie.CatalogItemId;

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
        when(this.deletionTransaction.finalizeManagedDeletion(CatalogItemId.of(42L), 7L))
                .thenReturn(Mono.empty());

        StepVerifier.create(this.consumer.consume(event(eventId)))
                .verifyComplete();

        verify(this.deletionTransaction).finalizeManagedDeletion(eq(CatalogItemId.of(42L)), eq(7L));
    }

    @Test
    void rejectsAnEventWithAnUnsupportedType() {
        String event = event(UUID.randomUUID()).replace("StoredObjectDeleted", "OtherEvent");

        StepVerifier.create(this.consumer.consume(event))
                .expectError(IllegalArgumentException.class)
                .verify();
    }

    @Test
    void rejectsInvalidContractFields() {
        String valid = event(UUID.randomUUID());
        String[] invalidEvents = {
                valid.replace("\"movieId\":42", "\"movieId\":0"),
                valid.replace("\"storageId\":7", "\"storageId\":-1"),
                valid.replace("\"objectKey\":\"objects/7\"", "\"objectKey\":\"\""),
                valid.replace("\"ownerUsername\":\"pepe\"", "\"ownerUsername\":\" \""),
                valid.replace("\"producer\":\"mvflix-storage\"", "\"producer\":\"other\""),
                valid.replace("\"id\":\"7\"", "\"id\":\"99\""),
                valid.replace("\"deletionStatus\":\"DELETED\"", "\"deletionStatus\":\"UNKNOWN\""),
                valid.replace("\"releasedBytes\":1024", "\"releasedBytes\":-1"),
                valid.replace(",\"releasedBytes\":1024", ""),
                valid.replace("2026-08-27T20:00:00Z", "not-a-timestamp")
        };

        for (String invalidEvent : invalidEvents) {
            StepVerifier.create(this.consumer.consume(invalidEvent))
                    .expectError(IllegalArgumentException.class)
                    .verify();
        }

        verify(this.deletionTransaction, never()).finalizeManagedDeletion(org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.anyLong());
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
