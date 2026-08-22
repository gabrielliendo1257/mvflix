package com.gcorp.service.app.mvflix_movies.catalog.application.port;

import java.util.List;

/** Detalle externo completo listo para fundir con la metadata de la pelicula. */
public record ExternalMovieDetail(
    long tmdbId,
    String title,
    String originalTitle,
    Integer year,
    List<String> genres,
    double popularity,
    int runtimeMinutes,
    String director,
    List<String> cast,
    String overview,
    String posterPath,
    String releaseDate,
    String country,
    String language) {}