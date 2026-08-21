package com.gcorp.service.app.mvflix_movies.presenter.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import com.gcorp.service.app.mvflix_movies.domain.movie.MediaKind;

import java.util.List;

/**
 * Edición manual de la metadata: campos {@code null} conservan el valor actual
 * (merge); las listas vacías limpian el valor. tmdbId no se edita por este
 * endpoint (se gestiona con enrich/re-enrich/unlink).
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
    @JsonProperty("poster_path") String posterPath,
    @JsonProperty("release_date") String releaseDate,
    String country,
    String language,
    List<String> awards,
    Double popularity,
    MediaKind kind) {}