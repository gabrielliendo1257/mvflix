package com.gcorp.service.app.mvflix_movies.presenter.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/** Cuerpo opcional del enrich: si viene tmdb_id, se salta el match automatico. */
public record EnrichMovieRequest(@JsonProperty("tmdb_id") Long tmdbId) {}