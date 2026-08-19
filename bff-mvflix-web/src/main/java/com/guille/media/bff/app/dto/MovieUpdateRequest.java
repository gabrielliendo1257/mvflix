package com.guille.media.bff.app.dto;

import java.util.List;

/**
 * Edición manual de la metadata: campos {@code null} conservan el valor actual
 * (merge); las listas vacías limpian el valor. tmdbId/posterPath/popularity no
 * se editan.
 */
public record MovieUpdateRequest(
    String title,
    String originalTitle,
    Integer year,
    List<String> genres,
    String duration,
    String director,
    List<String> cast,
    String overview,
    String releaseDate,
    String country,
    String language,
    List<String> awards) {}