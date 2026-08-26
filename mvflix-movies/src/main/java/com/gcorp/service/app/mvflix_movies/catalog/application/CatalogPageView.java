package com.gcorp.service.app.mvflix_movies.catalog.application;

import java.util.List;

/**
 * Página de la proyección owned: resumen operativo + filas + metadatos de
 * paginación real (LIMIT/OFFSET + COUNT en SQL, nunca paginación en memoria).
 */
public record CatalogPageView(
    Summary summary,
    List<CatalogItemView> items,
    int page,
    int size,
    long total,
    int totalPages) {

  public record Summary(long total, long ready, long needsAttention) {}

  public static CatalogPageView empty(int page, int size) {
    return new CatalogPageView(new Summary(0, 0, 0), List.of(), page, size, 0, 0);
  }
}
