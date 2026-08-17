package com.gcorp.service.app.mvflix_movies.domain.enrichment;

/** Resultado minimal de la busqueda: suficiente para confirmar el match. */
public record ExternalMovieSearch(
    long tmdbId,
    String title,
    Integer year,
    String posterPath,
    String releaseDate,
    String overview) {}