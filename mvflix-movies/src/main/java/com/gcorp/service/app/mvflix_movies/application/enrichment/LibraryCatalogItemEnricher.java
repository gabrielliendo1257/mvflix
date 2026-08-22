package com.gcorp.service.app.mvflix_movies.application.enrichment;

import com.gcorp.service.app.mvflix_movies.catalog.domain.movie.MovieId;
import com.gcorp.service.app.mvflix_movies.catalog.domain.movie.MovieNotFoundException;
import com.gcorp.service.app.mvflix_movies.catalog.domain.movie.MovieRepository;
import com.gcorp.service.app.mvflix_movies.library.application.port.CatalogItemEnricher;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;

import reactor.core.publisher.Mono;

/** Implementación de Enrichment para la capacidad solicitada por Library. */
@Service
@RequiredArgsConstructor
public class LibraryCatalogItemEnricher implements CatalogItemEnricher {

    private final MovieRepository movieRepository;
    private final EnrichMovieUseCase enrichMovieUseCase;

    @Override
    public Mono<Void> enrich(MovieId movieId, Long externalMetadataId) {
        return this.movieRepository
                .findById(movieId)
                .switchIfEmpty(Mono.error(new MovieNotFoundException(
                        "Movie not found: " + movieId.value())))
                .flatMap(movie -> this.enrichMovieUseCase.enrich(
                        movie, externalMetadataId))
                .then();
    }
}
