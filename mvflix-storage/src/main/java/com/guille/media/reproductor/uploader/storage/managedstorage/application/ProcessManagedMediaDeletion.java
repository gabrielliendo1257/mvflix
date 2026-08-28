package com.guille.media.reproductor.uploader.storage.managedstorage.application;

import com.guille.media.reproductor.uploader.storage.managedstorage.application.port.DeletionInboxRepository;

import org.springframework.stereotype.Service;

import reactor.core.publisher.Mono;

/** Processes the Movies integration event without leaking it into Storage use cases. */
@Service
public class ProcessManagedMediaDeletion {

    private final DeletionInboxRepository inboxRepository;
    private final StoredObjectDeletion storedObjectDeletion;

    public ProcessManagedMediaDeletion(
            DeletionInboxRepository inboxRepository, StoredObjectDeletion storedObjectDeletion) {
        this.inboxRepository = inboxRepository;
        this.storedObjectDeletion = storedObjectDeletion;
    }

    public Mono<Void> execute(ManagedMediaDeletionRequested event) {
        return this.inboxRepository.recordReceived(event.eventId())
                .then(this.inboxRepository.isCompleted(event.eventId()))
                .flatMap(completed -> completed ? Mono.empty() : this.storedObjectDeletion.execute(event))
                .onErrorResume(error -> this.inboxRepository.markFailed(event.eventId(), error.toString())
                        .then(Mono.error(error)));
    }
}
