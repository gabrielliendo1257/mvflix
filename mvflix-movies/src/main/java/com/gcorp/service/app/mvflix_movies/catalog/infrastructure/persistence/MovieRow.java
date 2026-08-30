package com.gcorp.service.app.mvflix_movies.catalog.infrastructure.persistence;

public record MovieRow(
    Long id,
    String ownerUsername,
    String title,
    String status,
    String enrichmentStatus,
    String metadata,
    String visibility,
    String[] sharedWith,
    String kind) {}
