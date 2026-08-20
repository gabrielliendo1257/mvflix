package com.gcorp.service.app.mvflix_movies.application.movie;

import java.util.List;

/**
 * Edición manual de la metadata de una película (sin pasar por la fuente externa).
 * Semántica de merge: los campos {@code null} conservan el valor actual de la movie;
 * las listas {@code null} se conservan y las listas vacías limpian el valor.
 * tmdbId no se edita (se gestiona con enrich/re-enrich/unlink).
 */
public record UpdateMovieCommand(
    String title,
    String originalTitle,
    Integer year,
    List<String> genres,
    String duration,
    String director,
    List<String> cast,
    String overview,
    String posterPath,
    String releaseDate,
    String country,
    String language,
    List<String> awards,
    Double popularity) {}