package com.gcorp.service.app.mvflix_movies.presenter.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Edición manual de la metadata: campos {@code null} conservan el valor actual
 * (merge); las listas vacías limpian el valor. tmdbId/posterPath/popularity no
 * se editan por este endpoint.
 */
public record UpdateMovieRequest(
    String title,
    String originalTitle,
    Integer year,
    List<String> genres,
    String duration,
    String director,
    List<String> cast,
    String overview,
    @JsonProperty("release_date") String releaseDate,
    String country,
    String language,
    List<String> awards) {}