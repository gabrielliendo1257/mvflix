package com.guille.media.bff.app.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record CreateMovieRequest(
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
    List<String> awards) {}
