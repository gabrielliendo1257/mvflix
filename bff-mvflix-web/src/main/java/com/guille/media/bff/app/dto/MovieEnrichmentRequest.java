package com.guille.media.bff.app.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/** Autocompletado elegido por el usuario: si trae tmdb_id, se salta el match automático. */
public record MovieEnrichmentRequest(@JsonProperty("tmdb_id") Long tmdbId) {}