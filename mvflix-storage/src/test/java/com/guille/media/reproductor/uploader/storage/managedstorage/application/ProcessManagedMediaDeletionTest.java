package com.guille.media.reproductor.uploader.storage.managedstorage.application;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.guille.media.reproductor.uploader.storage.managedstorage.application.port.DeletionInboxRepository;

import org.junit.jupiter.api.Test;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Instant;
import java.util.UUID;

class ProcessManagedMediaDeletionTest {

    private final DeletionInboxRepository inbox = org.mockito.Mockito.mock(DeletionInboxRepository.class);
    private final StoredObjectDeletion deletion = org.mockito.Mockito.mock(StoredObjectDeletion.class);
    private final ProcessManagedMediaDeletion process = new ProcessManagedMediaDeletion(inbox, deletion);

    @Test
    void newEventIsProcessedAndCompletedByTheDeletionFlow() {
        ManagedMediaDeletionRequested event = event();
        when(inbox.recordReceived(event.eventId())).thenReturn(Mono.empty());
        when(inbox.isCompleted(event.eventId())).thenReturn(Mono.just(false));
        when(deletion.execute(event)).thenReturn(Mono.empty());

        StepVerifier.create(process.execute(event)).verifyComplete();

        verify(deletion).execute(event);
        verify(inbox, never()).markFailed(any(), any());
    }

    @Test
    void completedEventIsANoop() {
        ManagedMediaDeletionRequested event = event();
        when(inbox.recordReceived(event.eventId())).thenReturn(Mono.empty());
        when(inbox.isCompleted(event.eventId())).thenReturn(Mono.just(true));

        StepVerifier.create(process.execute(event)).verifyComplete();

        verify(deletion, never()).execute(any());
    }

    @Test
    void failureLeavesInboxRetryableAndASecondDeliveryCanComplete() {
        ManagedMediaDeletionRequested event = event();
        when(inbox.recordReceived(event.eventId())).thenReturn(Mono.empty());
        when(inbox.isCompleted(event.eventId())).thenReturn(Mono.just(false));
        when(deletion.execute(event)).thenReturn(Mono.error(new RuntimeException("MinIO down")), Mono.empty());
        when(inbox.markFailed(event.eventId(), "java.lang.RuntimeException: MinIO down"))
                .thenReturn(Mono.empty());

        StepVerifier.create(process.execute(event)).expectError().verify();
        StepVerifier.create(process.execute(event)).verifyComplete();

        verify(deletion, org.mockito.Mockito.times(2)).execute(event);
        verify(inbox).markFailed(event.eventId(), "java.lang.RuntimeException: MinIO down");
    }

    private static ManagedMediaDeletionRequested event() {
        return new ManagedMediaDeletionRequested(UUID.randomUUID(), Instant.parse("2026-08-27T20:00:00Z"),
                42L, 7L, "pepe", "objects/7");
    }
}
