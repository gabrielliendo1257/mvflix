package com.guille.media.bff.app.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/** Preview de un candidato externo: lo que se aplicaría al enriquecer, sin persistir. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record MovieEnrichmentPreviewDto(
    String title,
    String originalTitle,
    Integer year,
    List<String> genres,
    Double popularity,
    String duration,
    String director,
    List<String> cast,
    String overview,
    @JsonProperty("poster_path") String posterPath,
    @JsonProperty("release_date") String releaseDate,
    String country,
    String language,
    @JsonProperty("tmdb_id") Long tmdbId) {}