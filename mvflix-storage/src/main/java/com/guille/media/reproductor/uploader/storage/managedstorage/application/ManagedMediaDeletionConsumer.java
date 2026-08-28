package com.guille.media.reproductor.uploader.storage.managedstorage.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.guille.media.reproductor.uploader.storage.managedstorage.application.command.request.DeleteStoredObjectCommand;
import com.guille.media.reproductor.uploader.storage.managedstorage.domain.port.DeletionInboxRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;

import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ManagedMediaDeletionConsumer {

  private final ObjectMapper objectMapper;
  private final DeletionInboxRepository inboxRepository;
  private final DeleteStoredObject deleteStoredObject;
  private final StoredObjectDeletedOutbox outbox;

  public Mono<Void> consume(String rawEvent) {
    return Mono.defer(() -> this.parse(rawEvent))
        .flatMap(this::consumeEvent)
        .doOnError(error -> log.warn("Managed deletion event failed", error));
  }

  private Mono<ManagedMediaDeletionRequested> parse(String rawEvent) {
    try {
      JsonNode root = this.objectMapper.readTree(rawEvent);
      if (!"ManagedMediaDeletionRequested".equals(root.path("eventType").asText())
          || root.path("eventVersion").asInt() != 1) {
        throw new IllegalArgumentException("Unsupported managed deletion event version");
      }
      JsonNode payload = root.path("payload");
      return Mono.just(new ManagedMediaDeletionRequested(
          UUID.fromString(root.path("eventId").asText()),
          InstantParser.parse(root.path("occurredAt").asText()),
          payload.path("movieId").asLong(),
          payload.path("storageId").asLong(),
          payload.path("ownerUsername").asText(),
          payload.path("objectKey").asText()));
    } catch (Exception error) {
      return Mono.error(new IllegalArgumentException("Invalid managed deletion event", error));
    }
  }

  private Mono<Void> consumeEvent(ManagedMediaDeletionRequested event) {
    return this.inboxRepository.recordReceived(event.eventId())
        .then(this.inboxRepository.isCompleted(event.eventId()))
        .flatMap(completed -> completed ? Mono.empty() : this.delete(event));
  }

  private Mono<Void> delete(ManagedMediaDeletionRequested event) {
    return this.deleteStoredObject
        .execute(new DeleteStoredObjectCommand(event.storageId()), event.ownerUsername(), event.objectKey(),
            result -> this.outbox.append(event, result).then(this.inboxRepository.markCompleted(event.eventId())))
        .then()
        .onErrorResume(error -> this.inboxRepository.markFailed(event.eventId(), error.toString()).then(Mono.error(error)));
  }

  private static final class InstantParser {
    private static java.time.Instant parse(String value) {
      return java.time.Instant.parse(value);
    }
  }
}
