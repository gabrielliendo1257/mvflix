package com.guille.media.reproductor.uploader.storage.managedstorage.application;

import com.guille.media.reproductor.uploader.storage.managedstorage.application.command.request.DeleteStoredObjectCommand;

import org.springframework.stereotype.Component;

import reactor.core.publisher.Mono;

/** Storage-only deletion adapter used by the managed deletion process. */
@Component
public class StoredObjectDeletion {

    private final DeleteStoredObject deleteStoredObject;
    private final ManagedDeletionTransaction transaction;

    public StoredObjectDeletion(DeleteStoredObject deleteStoredObject, ManagedDeletionTransaction transaction) {
        this.deleteStoredObject = deleteStoredObject;
        this.transaction = transaction;
    }

    public Mono<Void> execute(ManagedMediaDeletionRequested event) {
        return this.deleteStoredObject.execute(
                new DeleteStoredObjectCommand(event.storageId()),
                event.ownerUsername(), event.objectKey(),
                result -> this.transaction.complete(result, event)).then();
    }
}
