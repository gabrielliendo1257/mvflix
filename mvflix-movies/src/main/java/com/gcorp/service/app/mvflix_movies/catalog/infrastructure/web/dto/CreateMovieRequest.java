package com.gcorp.service.app.mvflix_movies.catalog.infrastructure.web.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import com.gcorp.service.app.mvflix_movies.catalog.domain.item.MediaKind;

import jakarta.validation.constraints.NotBlank;

import java.util.List;

public record CreateMovieRequest(
    @NotBlank String title,
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
    List<String> awards,
    MediaKind kind) {}
