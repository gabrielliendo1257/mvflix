package com.guille.media.bff.app.dto;

import java.util.List;

/**
 * Cambio de visibilidad en lote pedido por el front: ids directos (movieIds)
 * y/o librerias enteras (libraryIds). SHARED exige usernames.
 */
public record BulkVisibilityRequest(
        List<Long> movieIds,
        List<Long> libraryIds,
        String visibility,
        List<String> usernames) {
}