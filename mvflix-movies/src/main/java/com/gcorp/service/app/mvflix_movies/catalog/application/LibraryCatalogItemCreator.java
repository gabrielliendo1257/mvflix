package com.gcorp.service.app.mvflix_movies.catalog.application;

import com.gcorp.service.app.mvflix_movies.catalog.domain.movie.MediaKind;
import com.gcorp.service.app.mvflix_movies.catalog.domain.movie.Movie;
import com.gcorp.service.app.mvflix_movies.catalog.domain.movie.MovieId;
import com.gcorp.service.app.mvflix_movies.catalog.domain.movie.MovieMetadata;
import com.gcorp.service.app.mvflix_movies.catalog.domain.movie.MovieRepository;
import com.gcorp.service.app.mvflix_movies.library.application.port.CatalogItemCreator;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;

import reactor.core.publisher.Mono;

/** Implementación de Catalog para la creación solicitada por Library Ingestion. */
@Service
@RequiredArgsConstructor
public class LibraryCatalogItemCreator implements CatalogItemCreator {

    private final MovieRepository movieRepository;

    @Override
    public Mono<MovieId> createFromLibrary(
            String ownerUsername, String title, MediaKind kind) {
        Movie movie = Movie.fromLibraryAsset(
                ownerUsername, MovieMetadata.onlyTitle(title), kind);
        return this.movieRepository.save(movie).map(Movie::getId);
    }
}
