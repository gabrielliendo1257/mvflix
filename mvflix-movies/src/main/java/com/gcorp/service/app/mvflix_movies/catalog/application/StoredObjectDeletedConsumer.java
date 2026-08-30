package com.gcorp.service.app.mvflix_movies.catalog.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gcorp.service.app.mvflix_movies.catalog.application.port.StoredObjectDeleted;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;

import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class StoredObjectDeletedConsumer {

    private final ObjectMapper objectMapper;
    private final CatalogItemDeletionTransaction deletionTransaction;

    public Mono<Void> consume(String rawEvent) {
        return Mono.defer(() -> this.parse(rawEvent))
                .flatMap(event -> this.deletionTransaction
                        .finalizeManagedDeletion(
                                com.gcorp.service.app.mvflix_movies.catalog.domain.movie.CatalogItemId.of(event.movieId()),
                                event.storageId()))
                .doOnError(error -> log.warn("Stored object deletion confirmation failed", error));
    }

    private Mono<StoredObjectDeleted> parse(String rawEvent) {
        try {
            JsonNode root = this.objectMapper.readTree(rawEvent);
            if (root == null || !root.isObject()
                    || !text(root, "eventType").equals("StoredObjectDeleted")
                    || !root.path("eventVersion").canConvertToInt()
                    || root.path("eventVersion").asInt() != 1
                    || !text(root, "producer").equals("mvflix-storage")) {
                throw new IllegalArgumentException("Unsupported StoredObjectDeleted event version");
            }
            UUID.fromString(root.path("eventId").asText());
            Instant.parse(text(root, "occurredAt"));
            JsonNode aggregate = object(root, "aggregate");
            if (!text(aggregate, "type").equals("ManagedObject")) {
                throw new IllegalArgumentException("StoredObjectDeleted aggregate is not managed");
            }
            JsonNode payload = object(root, "payload");
            long storageId = positiveLong(payload, "storageId");
            long movieId = positiveLong(payload, "movieId");
            if (!text(aggregate, "id").equals(Long.toString(storageId))) {
                throw new IllegalArgumentException("StoredObjectDeleted aggregate does not match storage");
            }
            String deletionStatus = text(payload, "deletionStatus");
            if (!deletionStatus.equals("DELETED") && !deletionStatus.equals("ALREADY_ABSENT")) {
                throw new IllegalArgumentException("Unknown deletion status");
            }
            nonNegativeLong(payload, "releasedBytes");
            return Mono.just(new StoredObjectDeleted(
                    storageId,
                    movieId,
                    nonBlank(payload, "objectKey"),
                    nonBlank(payload, "ownerUsername")));
        } catch (Exception error) {
            return Mono.error(new IllegalArgumentException("Invalid StoredObjectDeleted event", error));
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

    private static long nonNegativeLong(JsonNode parent, String field) {
        JsonNode value = parent.path(field);
        if (!value.isIntegralNumber() || !value.canConvertToLong() || value.asLong() < 0) {
            throw new IllegalArgumentException("Invalid non-negative value: " + field);
        }
        return value.asLong();
    }
}
