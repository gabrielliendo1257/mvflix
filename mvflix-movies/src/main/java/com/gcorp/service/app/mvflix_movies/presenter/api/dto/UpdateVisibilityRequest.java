package com.gcorp.service.app.mvflix_movies.presenter.api.dto;

import com.gcorp.service.app.mvflix_movies.domain.movie.MovieVisibility;

/** Cambio de visibilidad de una pelicula del catalogo (solo el dueño). */
public record UpdateVisibilityRequest(MovieVisibility visibility) {}