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

class ManagedMediaDeletionConsumerTest {

  private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
  private final DeletionInboxRepository inbox = org.mockito.Mockito.mock(DeletionInboxRepository.class);
  private final DeleteStoredObject delete = org.mockito.Mockito.mock(DeleteStoredObject.class);
  private final ManagedMediaDeletionConsumer consumer =
      new ManagedMediaDeletionConsumer(objectMapper, inbox, delete);

  @Test
  void duplicateCompletedEventDoesNotDeleteOrPublishAgain() {
    UUID eventId = UUID.randomUUID();
    when(inbox.recordReceived(eventId)).thenReturn(Mono.empty());
    when(inbox.isCompleted(eventId)).thenReturn(Mono.just(true));

    StepVerifier.create(consumer.consume(event(eventId)))
        .verifyComplete();

    verify(delete, never()).execute(any(), any(), any(), any());
  }

  @Test
  void alreadyAbsentStillPublishesConfirmationAndCompletesInbox() {
    UUID eventId = UUID.randomUUID();
    when(inbox.recordReceived(eventId)).thenReturn(Mono.empty());
    when(inbox.isCompleted(eventId)).thenReturn(Mono.just(false));
    when(delete.execute(any(DeleteStoredObjectCommand.class), eq("pepe"), eq("objects/7"), any()))
        .thenReturn(Mono.just(new DeleteStoredObject.DeletionResult(0, "ALREADY_ABSENT")));

    StepVerifier.create(consumer.consume(event(eventId)))
        .verifyComplete();

    verify(delete).execute(any(DeleteStoredObjectCommand.class), eq("pepe"), eq("objects/7"), any());
    verify(inbox, never()).markFailed(any(), any());
  }

  @Test
  void rejectsInvalidContractFields() {
    String valid = event(UUID.randomUUID());
    String[] invalidEvents = {
        valid.replace("\"movieId\":42", "\"movieId\":0"),
        valid.replace("\"storageId\":7", "\"storageId\":-1"),
        valid.replace("\"ownerUsername\":\"pepe\"", "\"ownerUsername\":\" \""),
        valid.replace("\"objectKey\":\"objects/7\"", "\"objectKey\":\"\""),
        valid.replace("\"producer\":\"mvflix-movies\"", "\"producer\":\"other\""),
        valid.replace("\"id\":\"42\"", "\"id\":\"99\""),
        valid.replace("2026-08-27T20:00:00Z", "not-a-timestamp")
    };

    for (String invalidEvent : invalidEvents) {
      StepVerifier.create(consumer.consume(invalidEvent))
          .expectError(IllegalArgumentException.class)
          .verify();
    }

    verify(delete, never()).execute(any(), any(), any());
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
