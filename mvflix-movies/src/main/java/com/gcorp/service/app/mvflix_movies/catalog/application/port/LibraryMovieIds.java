package com.gcorp.service.app.mvflix_movies.catalog.application.port;

import com.gcorp.service.app.mvflix_movies.catalog.domain.movie.MovieId;

import reactor.core.publisher.Flux;

import java.util.List;

/** Capacidad de Library requerida por Catalog para operar sobre bibliotecas completas. */
public interface LibraryMovieIds {

    Flux<MovieId> findIdentifiedByLibraryIds(List<Long> libraryIds);
}
