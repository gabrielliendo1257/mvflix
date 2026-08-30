package com.gcorp.service.app.mvflix_movies.catalog.infrastructure.web.dto;

import com.gcorp.service.app.mvflix_movies.catalog.domain.item.CatalogItemVisibility;

import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * Acceso completo de una película en un solo request: visibilidad + lista de
 * compartidos (solo relevante para SHARED; el resto la ignora).
 */
public record UpdateMovieAccessRequest(
    @NotNull CatalogItemVisibility visibility,
    List<String> sharedWith) {}
