package com.guille.media.bff.app.dto;

/** Resultado de un lote de visibilidad tal como lo responde mvflix-movies. */
public record BulkVisibilityResultDto(int total, int updated, int failed) {
}