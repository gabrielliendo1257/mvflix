package com.gcorp.service.app.mvflix_movies.catalog.infrastructure.web.dto;

import com.gcorp.service.app.mvflix_movies.catalog.domain.item.CatalogItemVisibility;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.List;

/**
 * Cambio de visibilidad en lote: ids directos (movieIds) y/o librerias enteras
 * (libraryIds, se resuelven sus assets identificados). SHARED exige usernames.
 */
public record BulkVisibilityRequest(
        List<@Positive Long> movieIds,
        List<@Positive Long> libraryIds,
        @NotNull CatalogItemVisibility visibility,
        List<String> usernames) {
}
