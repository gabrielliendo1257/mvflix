package com.gcorp.service.app.mvflix_movies.domain.movie;

import java.util.List;

public record MovieMetadata(
    String title,
    String originalTitle,
    Integer year,
    List<String> genres,
    Double popularity,
    String duration,
    String director,
    List<String> cast,
    String overview,
    String posterPath,
    String releaseDate,
    String country,
    String language,
    List<String> awards,
    Long tmdbId) {}
