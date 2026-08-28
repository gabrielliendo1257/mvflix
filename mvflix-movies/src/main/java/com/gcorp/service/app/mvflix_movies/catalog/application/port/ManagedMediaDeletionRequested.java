package com.gcorp.service.app.mvflix_movies.catalog.application.port;

import java.time.Instant;
import java.util.UUID;

/** Evento versionado que Movies deja en la outbox después de entrar en DELETING. */
public record ManagedMediaDeletionRequested(
        UUID eventId,
        Instant occurredAt,
        long movieId,
        long storageId,
        String ownerUsername,
        String objectKey) {

    public static ManagedMediaDeletionRequested create(
            long movieId, long storageId, String ownerUsername, String objectKey) {
        return new ManagedMediaDeletionRequested(
                UUID.randomUUID(), Instant.now(), movieId, storageId, ownerUsername, objectKey);
    }
}
