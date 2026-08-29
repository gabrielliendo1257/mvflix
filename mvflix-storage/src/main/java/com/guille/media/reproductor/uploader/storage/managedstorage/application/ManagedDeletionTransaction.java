package com.guille.media.reproductor.uploader.storage.managedstorage.application;

import com.guille.media.reproductor.uploader.storage.managedstorage.application.port.DeletionInboxRepository;

import org.springframework.stereotype.Component;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.UUID;

import reactor.core.publisher.Mono;

/** Coordinates the managed deletion state, confirmation outbox and inbox. */
@Component
public class ManagedDeletionTransaction {

    private final StorageOutbox outbox;
    private final DeletionInboxRepository inboxRepository;

    public ManagedDeletionTransaction(
            StorageOutbox outbox,
            DeletionInboxRepository inboxRepository) {
        this.outbox = outbox;
        this.inboxRepository = inboxRepository;
    }

    public Mono<Void> complete(
            DeleteStoredObject.DeletionResult result,
            ManagedMediaDeletionRequested event) {
        StoredObjectDeletedIntegrationEvent confirmation = new StoredObjectDeletedIntegrationEvent(
                UUID.nameUUIDFromBytes(event.eventId().toString().getBytes(StandardCharsets.UTF_8)),
                1,
                Instant.now(),
                String.valueOf(event.storageId()),
                new StoredObjectDeletedIntegrationEvent.StoredObjectDeletedPayload(
                        event.movieId(),
                        event.storageId(),
                        event.objectKey(),
                        event.ownerUsername(),
                        result.releasedBytes(),
                        result.deletionStatus()));
        return this.outbox.append(confirmation)
                .then(this.inboxRepository.markCompleted(event.eventId()));
    }
}
