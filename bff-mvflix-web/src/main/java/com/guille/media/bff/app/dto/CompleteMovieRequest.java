package com.guille.media.bff.app.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Petición del front al complete orquestado: el front reporta qué sesión de subida
 * finalizó y cuánto midió el archivo que envió (para detectar basura/inconsistencias).
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record CompleteMovieRequest(Long storageId, Long sizeBytes) {}
