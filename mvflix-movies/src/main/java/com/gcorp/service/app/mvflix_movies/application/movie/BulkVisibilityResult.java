package com.gcorp.service.app.mvflix_movies.application.movie;

/** Resultado de un cambio de visibilidad en lote sobre el catálogo. */
public record BulkVisibilityResult(int total, int updated, int failed) {
}