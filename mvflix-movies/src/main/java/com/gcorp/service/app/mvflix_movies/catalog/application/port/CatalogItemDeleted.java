package com.gcorp.service.app.mvflix_movies.catalog.application.port;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record CatalogItemDeleted(UUID eventId, Instant occurredAt, String actorId,
        UUID correlationId, long movieId, String ownerUsername)
        implements CatalogSemanticEvent {
    public String eventType() { return "CatalogItemDeleted"; }
    public int eventVersion() { return 1; }
    public String aggregateType() { return "CatalogItem"; }
    public String aggregateId() { return String.valueOf(movieId); }
    public Object payload() { return Map.of("movieId", movieId, "ownerUsername", ownerUsername); }
}
