package com.gcorp.service.app.mvflix_movies.library.infrastructure.web.dto;

/**
 * Activo de biblioteca del catálogo. {@code present} refleja la presencia en
 * disco (ortogonal al estado de identificación): sin él, los consumidores no
 * pueden distinguir un LOCAL reproducible de un archivo desaparecido.
 */
public record MediaAssetResponse(
    Long id,
    Long libraryId,
    String relativePath,
    long size,
    String mimeType,
    String status,
    Boolean present,
    Long movieId) {}
