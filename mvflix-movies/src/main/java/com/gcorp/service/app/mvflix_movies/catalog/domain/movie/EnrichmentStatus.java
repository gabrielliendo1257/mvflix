package com.gcorp.service.app.mvflix_movies.catalog.domain.movie;

/**
 * Estado de enriquecimiento del catalogo, ortogonal a {@link CatalogItemStatus}:
 * una pelicula puede estar READY (lista para reproducir) y aun RAW (sin
 * metadatos externos), o ENRICHED en DRAFT (metadata importada antes del media).
 */
public enum EnrichmentStatus {

    /** Solo la metadata minima que aporto el usuario. */
    RAW,

    /** Enriquecimiento incompleto (ej: matcheo ambiguo, solo creditos, etc). */
    PARTIAL,

    /** Metadatos de fuentes externas aplicados de forma completa. */
    ENRICHED
}