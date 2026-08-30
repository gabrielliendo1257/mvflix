package com.gcorp.service.app.mvflix_movies.catalog.application.port;

import java.time.Instant;
import java.util.UUID;
import java.util.Map;

/** Evento versionado que Movies deja en la outbox después de entrar en DELETING. */
public record ManagedMediaDeletionRequested(
        UUID eventId,
        Instant occurredAt,
        String actorId,
        UUID correlationId,
        long movieId,
        long storageId,
        String ownerUsername,
        String objectKey) implements CatalogSemanticEvent {

    public String eventType() { return "ManagedMediaDeletionRequested"; }
    public int eventVersion() { return 1; }
    public String aggregateType() { return "Movie"; }
    public String aggregateId() { return String.valueOf(movieId); }
    public Object payload() { return Map.of("movieId", movieId, "storageId", storageId,
            "ownerUsername", ownerUsername, "objectKey", objectKey); }

    public static ManagedMediaDeletionRequested create(
            long movieId, long storageId, String ownerUsername, String objectKey) {
        return create(movieId, storageId, ownerUsername, objectKey, "system", UUID.randomUUID());
    }

    public static ManagedMediaDeletionRequested create(long movieId, long storageId,
            String ownerUsername, String objectKey, String actorId, UUID correlationId) {
        return new ManagedMediaDeletionRequested(UUID.randomUUID(), Instant.now(), actorId,
                correlationId, movieId, storageId, ownerUsername, objectKey);
    }

    public static ManagedMediaDeletionRequested create(UUID eventId, long movieId, long storageId,
            String ownerUsername, String objectKey, String actorId, UUID correlationId) {
        return new ManagedMediaDeletionRequested(eventId, Instant.now(), actorId, correlationId,
                movieId, storageId, ownerUsername, objectKey);
    }
}
