package com.gcorp.service.app.mvflix_movies.infrastructure.database;

public record MovieRow(
    Long id,
    String ownerUsername,
    String title,
    String status,
    String enrichmentStatus,
    Long objectId,
    String metadata,
    String visibility,
    String[] sharedWith,
    String kind) {}