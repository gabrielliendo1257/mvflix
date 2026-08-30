package com.gcorp.service.app.mvflix_movies.catalog.infrastructure.web.dto;

import com.gcorp.service.app.mvflix_movies.catalog.domain.item.CatalogItemVisibility;

import jakarta.validation.constraints.NotNull;

/** Cambio de visibilidad de una pelicula del catalogo (solo el dueño). */
public record UpdateVisibilityRequest(@NotNull CatalogItemVisibility visibility) {}
