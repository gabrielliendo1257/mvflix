package com.gcorp.service.app.mvflix_movies.library.application.port;

import com.gcorp.service.app.mvflix_movies.domain.movie.MediaKind;
import com.gcorp.service.app.mvflix_movies.domain.movie.Movie;

import reactor.core.publisher.Mono;

/** Contrato que Library necesita para incorporar un archivo al catálogo. */
public interface CatalogItemCreator {

    Mono<Movie> createFromLibrary(String ownerUsername, String title, MediaKind kind);
}
