package com.guille.media.reproductor.uploader.storage.managedstorage.application;

import com.guille.media.reproductor.uploader.storage.managedstorage.domain.model.StoreObject;
import com.guille.media.reproductor.uploader.storage.managedstorage.domain.model.StoreObject.StorageSessionStatus;
import com.guille.media.reproductor.uploader.storage.managedstorage.domain.port.DeletionInboxRepository;

import org.springframework.stereotype.Component;

import reactor.core.publisher.Mono;

/** Coordinates the managed deletion state, confirmation outbox and inbox. */
@Component
public class ManagedDeletionTransaction {

    private final TerminalUploadTransition terminalTransition;
    private final StoredObjectDeletedOutbox outbox;
    private final DeletionInboxRepository inboxRepository;

    public ManagedDeletionTransaction(
            TerminalUploadTransition terminalTransition,
            StoredObjectDeletedOutbox outbox,
            DeletionInboxRepository inboxRepository) {
        this.terminalTransition = terminalTransition;
        this.outbox = outbox;
        this.inboxRepository = inboxRepository;
    }

    public Mono<Void> complete(
            StoreObject object,
            DeleteStoredObject.DeletionResult result,
            ManagedMediaDeletionRequested event) {
        if ("ALREADY_ABSENT".equals(result.deletionStatus())) {
            return this.outbox.append(event, result)
                    .then(this.inboxRepository.markCompleted(event.eventId()));
        }
        return this.terminalTransition
                .transitionAndRelease(object, StorageSessionStatus.COMPLETED,
                        updated -> this.outbox.append(event, result)
                                .then(this.inboxRepository.markCompleted(event.eventId())))
                .then();
    }
}
