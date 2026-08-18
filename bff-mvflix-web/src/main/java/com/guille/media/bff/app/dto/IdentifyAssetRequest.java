package com.guille.media.bff.app.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Identificación de un activo en un solo paso: el título es opcional (se deriva
 * del filename) y tmdb_id autocompleta la metadata si el usuario eligió candidato.
 */
public record IdentifyAssetRequest(
    String title, @JsonProperty("tmdb_id") Long tmdbId) {}
