package com.gcorp.service.app.mvflix_movies.library.application.port;

import com.gcorp.service.app.mvflix_movies.catalog.domain.movie.MediaKind;
import com.gcorp.service.app.mvflix_movies.catalog.domain.movie.MovieId;

import reactor.core.publisher.Mono;

/** Contrato que Library necesita para incorporar un archivo al catálogo. */
public interface CatalogItemCreator {

    Mono<MovieId> createFromLibrary(String ownerUsername, String title, MediaKind kind);
}
