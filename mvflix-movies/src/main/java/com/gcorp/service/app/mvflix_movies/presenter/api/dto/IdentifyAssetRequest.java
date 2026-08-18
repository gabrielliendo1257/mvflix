package com.gcorp.service.app.mvflix_movies.presenter.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Identificacion de un activo de biblioteca en un solo paso: titulo manual
 * y, opcionalmente, un candidato TMDB ya elegido para autocompletar la metadata.
 */
public record IdentifyAssetRequest(String title, @JsonProperty("tmdb_id") Long tmdbId) {}
