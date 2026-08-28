package com.gcorp.service.app.mvflix_movies.catalog.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gcorp.service.app.mvflix_movies.catalog.application.port.StoredObjectDeleted;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;

import reactor.core.publisher.Mono;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class StoredObjectDeletedConsumer {

    private final ObjectMapper objectMapper;
    private final MovieDeletionTransaction deletionTransaction;

    public Mono<Void> consume(String rawEvent) {
        return Mono.defer(() -> this.parse(rawEvent))
                .flatMap(event -> this.deletionTransaction
                        .finalizeManagedDeletion(
                                com.gcorp.service.app.mvflix_movies.catalog.domain.movie.MovieId.of(event.movieId()),
                                event.storageId()))
                .doOnError(error -> log.warn("Stored object deletion confirmation failed", error));
    }

    private Mono<StoredObjectDeleted> parse(String rawEvent) {
        try {
            JsonNode root = this.objectMapper.readTree(rawEvent);
            if (!"StoredObjectDeleted".equals(root.path("eventType").asText())
                    || root.path("eventVersion").asInt() != 1) {
                throw new IllegalArgumentException("Unsupported StoredObjectDeleted event version");
            }
            UUID.fromString(root.path("eventId").asText());
            JsonNode aggregate = root.path("aggregate");
            if (!"ManagedObject".equals(aggregate.path("type").asText())) {
                throw new IllegalArgumentException("StoredObjectDeleted aggregate is not managed");
            }
            JsonNode payload = root.path("payload");
            return Mono.just(new StoredObjectDeleted(
                    payload.path("storageId").asLong(),
                    payload.path("movieId").asLong(0),
                    payload.path("objectKey").asText(null),
                    payload.path("ownerUsername").asText(null)));
        } catch (Exception error) {
            return Mono.error(new IllegalArgumentException("Invalid StoredObjectDeleted event", error));
        }
    }
}
