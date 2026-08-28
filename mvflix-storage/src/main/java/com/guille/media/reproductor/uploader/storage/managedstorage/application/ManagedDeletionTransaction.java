package com.guille.media.reproductor.uploader.storage.managedstorage.application;

import com.guille.media.reproductor.uploader.storage.managedstorage.application.port.DeletionInboxRepository;

import org.springframework.stereotype.Component;

import reactor.core.publisher.Mono;

/** Coordinates the managed deletion state, confirmation outbox and inbox. */
@Component
public class ManagedDeletionTransaction {

    private final StoredObjectDeletedOutbox outbox;
    private final DeletionInboxRepository inboxRepository;

    public ManagedDeletionTransaction(
            StoredObjectDeletedOutbox outbox,
            DeletionInboxRepository inboxRepository) {
        this.outbox = outbox;
        this.inboxRepository = inboxRepository;
    }

    public Mono<Void> complete(
            DeleteStoredObject.DeletionResult result,
            ManagedMediaDeletionRequested event) {
        return this.outbox.append(event, result)
                .then(this.inboxRepository.markCompleted(event.eventId()));
    }
}
