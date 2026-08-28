package com.guille.media.reproductor.uploader.storage.managedstorage.application;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.guille.media.reproductor.uploader.storage.managedstorage.application.command.request.DeleteStoredObjectCommand;

import org.junit.jupiter.api.Test;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Instant;
import java.util.UUID;
import java.util.function.Function;

class StoredObjectDeletionTest {

    private final DeleteStoredObject delete = org.mockito.Mockito.mock(DeleteStoredObject.class);
    private final ManagedDeletionTransaction transaction = org.mockito.Mockito.mock(ManagedDeletionTransaction.class);
    private final StoredObjectDeletion deletion = new StoredObjectDeletion(delete, transaction);

    @Test
    void newEventDeletesAndCompletesThroughTheTransaction() {
        ManagedMediaDeletionRequested event = event();
        when(delete.execute(any(DeleteStoredObjectCommand.class), eq("pepe"), eq("objects/7"), any()))
                .thenAnswer(invocation -> confirmation(invocation, event, 1024L, "DELETED"));
        when(transaction.complete(any(), eq(event))).thenReturn(Mono.empty());

        StepVerifier.create(deletion.execute(event)).verifyComplete();

        verify(transaction).complete(new DeleteStoredObject.DeletionResult(1024L, "DELETED"), event);
    }

    @Test
    void alreadyDeletedPublishesAnAlreadyAbsentConfirmation() {
        ManagedMediaDeletionRequested event = event();
        when(delete.execute(any(DeleteStoredObjectCommand.class), eq("pepe"), eq("objects/7"), any()))
                .thenAnswer(invocation -> confirmation(invocation, event, 0L, "ALREADY_ABSENT"));
        when(transaction.complete(any(), eq(event))).thenReturn(Mono.empty());

        StepVerifier.create(deletion.execute(event)).verifyComplete();

        verify(transaction).complete(new DeleteStoredObject.DeletionResult(0L, "ALREADY_ABSENT"), event);
    }

    @Test
    void storageFailureDoesNotConfirmTheEvent() {
        ManagedMediaDeletionRequested event = event();
        when(delete.execute(any(DeleteStoredObjectCommand.class), eq("pepe"), eq("objects/7"), any()))
                .thenReturn(Mono.error(new RuntimeException("MinIO down")));

        StepVerifier.create(deletion.execute(event)).expectErrorMessage("MinIO down").verify();

        verify(transaction, never()).complete(any(), any());
    }

    @Test
    void confirmationFailureIsPropagatedForRetry() {
        ManagedMediaDeletionRequested event = event();
        when(delete.execute(any(DeleteStoredObjectCommand.class), eq("pepe"), eq("objects/7"), any()))
                .thenAnswer(invocation -> confirmation(invocation, event, 1024L, "DELETED"));
        when(transaction.complete(any(), eq(event))).thenReturn(Mono.error(new RuntimeException("outbox down")));

        StepVerifier.create(deletion.execute(event)).expectErrorMessage("outbox down").verify();
    }

    private static Mono<DeleteStoredObject.DeletionResult> confirmation(
            org.mockito.invocation.InvocationOnMock invocation,
            ManagedMediaDeletionRequested event, long bytes, String status) {
        Function<DeleteStoredObject.DeletionResult, Mono<Void>> callback = invocation.getArgument(3);
        DeleteStoredObject.DeletionResult result = new DeleteStoredObject.DeletionResult(bytes, status);
        return callback.apply(result).thenReturn(result);
    }

    private static ManagedMediaDeletionRequested event() {
        return new ManagedMediaDeletionRequested(UUID.randomUUID(), Instant.parse("2026-08-27T20:00:00Z"),
                42L, 7L, "pepe", "objects/7");
    }
}
