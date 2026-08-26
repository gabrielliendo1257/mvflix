package com.guille.media.bff.experience.media.application;

import java.util.List;

/**
 * Parche de metadata editable por el dueño. Campos {@code null} conservan el
 * valor actual (merge); listas vacías limpian. {@code tmdbId} NO se edita
 * aquí: se gestiona con link/unlink de proveedor.
 */
public record MetadataPatch(
    String title,
    String originalTitle,
    Integer year,
    List<String> genres,
    String duration,
    String director,
    List<String> cast,
    String overview,
    String posterUrl,
    String releaseDate,
    String country,
    String language,
    List<String> awards,
    Double popularity,
    String kind) {}
