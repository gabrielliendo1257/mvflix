package com.guille.media.reproductor.uploader.storage.managedstorage.application;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.guille.media.reproductor.uploader.storage.managedstorage.application.command.request.DeleteStoredObjectCommand;
import com.guille.media.reproductor.uploader.storage.managedstorage.domain.port.DeletionInboxRepository;

import org.junit.jupiter.api.Test;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.UUID;
import java.util.function.Function;

class ManagedMediaDeletionConsumerTest {

  private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
  private final DeletionInboxRepository inbox = org.mockito.Mockito.mock(DeletionInboxRepository.class);
  private final DeleteStoredObject delete = org.mockito.Mockito.mock(DeleteStoredObject.class);
  private final StoredObjectDeletedOutbox outbox = org.mockito.Mockito.mock(StoredObjectDeletedOutbox.class);
  private final ManagedMediaDeletionConsumer consumer =
      new ManagedMediaDeletionConsumer(objectMapper, inbox, delete, outbox);

  @Test
  void duplicateCompletedEventDoesNotDeleteOrPublishAgain() {
    UUID eventId = UUID.randomUUID();
    when(inbox.recordReceived(eventId)).thenReturn(Mono.empty());
    when(inbox.isCompleted(eventId)).thenReturn(Mono.just(true));

    StepVerifier.create(consumer.consume(event(eventId)))
        .verifyComplete();

    verify(delete, never()).execute(any(), any(), any());
    verify(outbox, never()).append(any(), any());
  }

  @Test
  void alreadyAbsentStillPublishesConfirmationAndCompletesInbox() {
    UUID eventId = UUID.randomUUID();
    when(inbox.recordReceived(eventId)).thenReturn(Mono.empty());
    when(inbox.isCompleted(eventId)).thenReturn(Mono.just(false));
    when(delete.execute(any(DeleteStoredObjectCommand.class), eq("pepe"), eq("objects/7"),
        org.mockito.ArgumentMatchers.<Function<DeleteStoredObject.DeletionResult, Mono<Void>>>any()))
        .thenAnswer(invocation -> invocation.<Function<DeleteStoredObject.DeletionResult, Mono<Void>>>getArgument(3)
            .apply(new DeleteStoredObject.DeletionResult(0, "ALREADY_ABSENT"))
            .thenReturn(new DeleteStoredObject.DeletionResult(0, "ALREADY_ABSENT")));
    when(outbox.append(any(), any())).thenReturn(Mono.empty());
    when(inbox.markCompleted(eventId)).thenReturn(Mono.empty());

    StepVerifier.create(consumer.consume(event(eventId)))
        .verifyComplete();

    verify(outbox).append(any(), any());
    verify(inbox).markCompleted(eventId);
    verify(inbox, never()).markFailed(any(), any());
  }

  private static String event(UUID eventId) {
    return """
        {
          "eventId":"%s",
          "eventType":"ManagedMediaDeletionRequested",
          "eventVersion":1,
          "occurredAt":"2026-08-27T20:00:00Z",
          "producer":"mvflix-movies",
          "aggregate":{"type":"Movie","id":"42"},
          "payload":{"movieId":42,"storageId":7,"ownerUsername":"pepe","objectKey":"objects/7"}
        }
        """.formatted(eventId);
  }
}
