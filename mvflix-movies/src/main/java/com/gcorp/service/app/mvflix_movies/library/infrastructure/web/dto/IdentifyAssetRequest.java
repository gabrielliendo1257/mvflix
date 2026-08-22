package com.gcorp.service.app.mvflix_movies.library.infrastructure.web.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import com.gcorp.service.app.mvflix_movies.catalog.domain.movie.MediaKind;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

/**
 * Identificacion de un activo de biblioteca en un solo paso: titulo manual,
 * tipo de contenido y, opcionalmente, un candidato TMDB ya elegido para
 * autocompletar la metadata (solo si kind = MOVIE).
 */
public record IdentifyAssetRequest(
    @NotBlank String title,
    @JsonProperty("tmdb_id") @Positive Long tmdbId,
    MediaKind kind) {}
