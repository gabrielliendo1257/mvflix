package com.gcorp.service.app.mvflix_movies.catalog.application;

import com.gcorp.service.app.mvflix_movies.catalog.domain.item.CatalogItemKind;
import com.gcorp.service.app.mvflix_movies.catalog.domain.metadata.MovieMetadata;
import com.gcorp.service.app.mvflix_movies.catalog.domain.access.Visibility;

import java.util.List;
import java.util.UUID;

/**
 * Intención completa del alta guiada: draft identificado (metadata + tmdbId)
 * con acceso inicial aplicado por Movies en una sola unidad.
 */
public record CreateIdentifiedDraftCommand(
        MovieMetadata metadata,
        CatalogItemKind kind,
        Visibility visibility,
        List<String> sharedWith,
        String idempotencyKey,
        UUID correlationId) {

    public CreateIdentifiedDraftCommand(MovieMetadata metadata, CatalogItemKind kind,
                                        Visibility visibility, List<String> sharedWith) {
        this(metadata, kind, visibility, sharedWith, null, null);
    }

    public CreateIdentifiedDraftCommand(MovieMetadata metadata, CatalogItemKind kind,
                                        Visibility visibility, List<String> sharedWith,
                                        String idempotencyKey) {
        this(metadata, kind, visibility, sharedWith, idempotencyKey, null);
    }
}
