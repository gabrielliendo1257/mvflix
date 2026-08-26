package com.gcorp.service.app.mvflix_movies.catalog.infrastructure.web.dto;

import com.gcorp.service.app.mvflix_movies.catalog.domain.movie.MovieVisibility;

import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * Acceso completo de una película en un solo request: visibilidad + lista de
 * compartidos (solo relevante para SHARED; el resto la ignora).
 */
public record UpdateMovieAccessRequest(
    @NotNull MovieVisibility visibility,
    List<String> sharedWith) {}
