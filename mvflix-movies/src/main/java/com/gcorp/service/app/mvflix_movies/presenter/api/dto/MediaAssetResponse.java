package com.gcorp.service.app.mvflix_movies.presenter.api.dto;

public record MediaAssetResponse(
    Long id,
    Long storageId,
    String relativePath,
    long size,
    String mimeType,
    String status,
    Long movieId) {}
