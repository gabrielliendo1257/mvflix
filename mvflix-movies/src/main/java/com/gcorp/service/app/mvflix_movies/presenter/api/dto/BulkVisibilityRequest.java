package com.gcorp.service.app.mvflix_movies.presenter.api.dto;

import com.gcorp.service.app.mvflix_movies.domain.movie.MovieVisibility;

import java.util.List;

/**
 * Cambio de visibilidad en lote: ids directos (movieIds) y/o librerias enteras
 * (libraryIds, se resuelven sus assets identificados). SHARED exige usernames.
 */
public record BulkVisibilityRequest(
        List<Long> movieIds,
        List<Long> libraryIds,
        MovieVisibility visibility,
        List<String> usernames) {
}