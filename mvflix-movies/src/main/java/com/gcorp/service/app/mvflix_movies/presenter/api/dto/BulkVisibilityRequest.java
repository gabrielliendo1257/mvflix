package com.gcorp.service.app.mvflix_movies.presenter.api.dto;

import com.gcorp.service.app.mvflix_movies.domain.movie.MovieVisibility;

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
        @NotNull MovieVisibility visibility,
        List<String> usernames) {
}
