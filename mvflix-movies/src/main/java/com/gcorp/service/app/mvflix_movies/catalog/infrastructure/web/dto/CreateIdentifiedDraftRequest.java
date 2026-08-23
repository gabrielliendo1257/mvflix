package com.gcorp.service.app.mvflix_movies.catalog.infrastructure.web.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * Alta guiada (Add Media): metadata del preview + identidad TMDB + acceso
 * inicial. Movies aplica y valida todo como una sola unidad; el BFF coordina.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record CreateIdentifiedDraftRequest(
    @Valid @NotNull CreateMovieRequest draft,
    @JsonProperty("tmdb_id") Long tmdbId,
    String visibility,
    List<String> sharedWith) {}
