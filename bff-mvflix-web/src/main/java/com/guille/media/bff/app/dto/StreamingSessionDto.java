package com.guille.media.bff.app.dto;

public record StreamingSessionDto(
    String uploadId,
    String streamingUrl,
    String storageKey,
    String expiresAt,
    String method) {}
