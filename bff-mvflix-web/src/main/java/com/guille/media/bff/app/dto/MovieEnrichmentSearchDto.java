package com.guille.media.bff.app.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/** Candidato de la búsqueda externa: lo que el usuario ve antes de elegir. */
public record MovieEnrichmentSearchDto(
    @JsonProperty("tmdb_id") Long tmdbId,
    String title,
    Integer year,
    @JsonProperty("poster_path") String posterPath,
    @JsonProperty("release_date") String releaseDate,
    String overview) {}