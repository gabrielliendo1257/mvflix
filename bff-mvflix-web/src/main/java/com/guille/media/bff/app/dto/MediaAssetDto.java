package com.guille.media.bff.app.dto;

/** Activo de biblioteca (catálogo del media server). */
public record MediaAssetDto(
    Long id,
    Long libraryId,
    String relativePath,
    long size,
    String mimeType,
    String status,
    Long movieId) {}
