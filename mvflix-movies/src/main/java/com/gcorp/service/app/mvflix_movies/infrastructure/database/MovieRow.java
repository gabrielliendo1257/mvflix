package com.gcorp.service.app.mvflix_movies.infrastructure.database;

public record MovieRow(
    Long id,
    String ownerUsername,
    String title,
    String status,
    Long objectId,
    String objectKey,
    String metadata) {}
