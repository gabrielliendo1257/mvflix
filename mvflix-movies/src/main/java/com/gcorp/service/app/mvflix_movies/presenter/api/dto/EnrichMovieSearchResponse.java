package com.gcorp.service.app.mvflix_movies.presenter.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/** Candidato de la busqueda en la fuente externa: lo que ve el usuario antes de elegir. */
public record EnrichMovieSearchResponse(
        @JsonProperty("tmdb_id") Long tmdbId,
        String title,
        Integer year,
        @JsonProperty("poster_path") String posterPath,
        @JsonProperty("release_date") String releaseDate,
        String overview) {}