package com.gcorp.service.app.mvflix_movies.library.infrastructure.web.dto;

public record MediaAssetResponse(
    Long id,
    Long libraryId,
    String relativePath,
    long size,
    String mimeType,
    String status,
    Long movieId) {}
