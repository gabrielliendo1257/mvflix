package com.guille.media.reproductor.uploader.storage.managedstorage.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.guille.media.reproductor.uploader.storage.managedstorage.application.command.request.DeleteStoredObjectCommand;
import com.guille.media.reproductor.uploader.storage.managedstorage.domain.port.DeletionInboxRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;

import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ManagedMediaDeletionConsumer {

  private final ObjectMapper objectMapper;
  private final DeletionInboxRepository inboxRepository;
  private final DeleteStoredObject deleteStoredObject;

  public Mono<Void> consume(String rawEvent) {
    return Mono.defer(() -> this.parse(rawEvent))
        .flatMap(this::consumeEvent)
        .doOnError(error -> log.warn("Managed deletion event failed", error));
  }

  private Mono<ManagedMediaDeletionRequested> parse(String rawEvent) {
    try {
      JsonNode root = this.objectMapper.readTree(rawEvent);
      if (root == null || !root.isObject()
          || !text(root, "eventType").equals("ManagedMediaDeletionRequested")
          || !root.path("eventVersion").canConvertToInt()
          || root.path("eventVersion").asInt() != 1
          || !text(root, "producer").equals("mvflix-movies")) {
        throw new IllegalArgumentException("Unsupported managed deletion event version");
      }
      UUID eventId = UUID.fromString(text(root, "eventId"));
      Instant occurredAt = Instant.parse(text(root, "occurredAt"));
      JsonNode aggregate = object(root, "aggregate");
      if (!text(aggregate, "type").equals("Movie")) {
        throw new IllegalArgumentException("Managed deletion aggregate is not a movie");
      }
      JsonNode payload = root.path("payload");
      long movieId = positiveLong(payload, "movieId");
      if (!text(aggregate, "id").equals(Long.toString(movieId))) {
        throw new IllegalArgumentException("Managed deletion aggregate does not match movie");
      }
      return Mono.just(new ManagedMediaDeletionRequested(
          eventId,
          occurredAt,
          movieId,
          positiveLong(payload, "storageId"),
          nonBlank(payload, "ownerUsername"),
          nonBlank(payload, "objectKey")));
    } catch (Exception error) {
      return Mono.error(new IllegalArgumentException("Invalid managed deletion event", error));
    }
  }

  private static JsonNode object(JsonNode parent, String field) {
    JsonNode value = parent.path(field);
    if (!value.isObject()) {
      throw new IllegalArgumentException("Missing object field: " + field);
    }
    return value;
  }

  private static String text(JsonNode parent, String field) {
    JsonNode value = parent.path(field);
    if (!value.isTextual() || value.textValue().isBlank()) {
      throw new IllegalArgumentException("Missing text field: " + field);
    }
    return value.textValue();
  }

  private static String nonBlank(JsonNode parent, String field) {
    return text(parent, field);
  }

  private static long positiveLong(JsonNode parent, String field) {
    JsonNode value = parent.path(field);
    if (!value.isIntegralNumber() || !value.canConvertToLong() || value.asLong() <= 0) {
      throw new IllegalArgumentException("Invalid positive ID: " + field);
    }
    return value.asLong();
  }

  private Mono<Void> consumeEvent(ManagedMediaDeletionRequested event) {
    return this.inboxRepository.recordReceived(event.eventId())
        .then(this.inboxRepository.isCompleted(event.eventId()))
        .flatMap(completed -> completed ? Mono.empty() : this.delete(event));
  }

  private Mono<Void> delete(ManagedMediaDeletionRequested event) {
    return this.deleteStoredObject
        .execute(new DeleteStoredObjectCommand(event.storageId()), event.ownerUsername(), event.objectKey(), event)
        .then()
        .onErrorResume(error -> this.inboxRepository.markFailed(event.eventId(), error.toString()).then(Mono.error(error)));
  }

}
