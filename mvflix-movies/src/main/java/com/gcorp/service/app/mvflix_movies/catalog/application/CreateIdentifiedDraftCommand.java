package com.gcorp.service.app.mvflix_movies.catalog.application;

import com.gcorp.service.app.mvflix_movies.catalog.domain.movie.MediaKind;
import com.gcorp.service.app.mvflix_movies.catalog.domain.movie.MovieMetadata;
import com.gcorp.service.app.mvflix_movies.catalog.domain.movie.MovieVisibility;

import java.util.List;

/**
 * Intención completa del alta guiada: draft identificado (metadata + tmdbId)
 * con acceso inicial aplicado por Movies en una sola unidad.
 */
public record CreateIdentifiedDraftCommand(
        MovieMetadata metadata,
        MediaKind kind,
        MovieVisibility visibility,
        List<String> sharedWith) {}
