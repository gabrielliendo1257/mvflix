package com.gcorp.service.app.mvflix_movies.presenter.api.dto;

/** Resultado de un cambio de visibilidad en lote. */
public record BulkVisibilityResponse(int total, int updated, int failed) {
}