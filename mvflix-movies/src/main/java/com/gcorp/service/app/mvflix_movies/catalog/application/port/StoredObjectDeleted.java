package com.gcorp.service.app.mvflix_movies.catalog.application.port;

public record StoredObjectDeleted(long storageId, long movieId, String objectKey, String ownerUsername) {}
