package com.gcorp.service.app.mvflix_movies.catalog.application;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.gcorp.service.app.mvflix_movies.catalog.domain.movie.EnrichmentStatus;
import com.gcorp.service.app.mvflix_movies.catalog.domain.movie.MediaKind;
import com.gcorp.service.app.mvflix_movies.catalog.domain.movie.Movie;
import com.gcorp.service.app.mvflix_movies.catalog.domain.movie.MovieId;
import com.gcorp.service.app.mvflix_movies.catalog.domain.movie.MovieNotFoundException;
import com.gcorp.service.app.mvflix_movies.catalog.domain.movie.MovieRepository;
import com.gcorp.service.app.mvflix_movies.catalog.domain.movie.MovieStatus;
import com.gcorp.service.app.mvflix_movies.catalog.domain.movie.MovieVisibility;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.Set;

@ExtendWith(MockitoExtension.class)
class LibraryCatalogItemEnricherTest {

    @Mock private MovieRepository movieRepository;
    @Mock private EnrichMovieUseCase enrichMovieUseCase;

    @InjectMocks private LibraryCatalogItemEnricher enricher;

    @Test
    void enrichesTheCatalogItemWithoutExposingCatalogInternalsToLibrary() {
        Movie movie = movie();
        when(this.movieRepository.findById(MovieId.of(50L))).thenReturn(Mono.just(movie));
        when(this.enrichMovieUseCase.enrich(movie, 123L)).thenReturn(Mono.just(movie));

        StepVerifier.create(this.enricher.enrich(MovieId.of(50L), 123L))
                .verifyComplete();

        verify(this.enrichMovieUseCase).enrich(movie, 123L);
    }

    @Test
    void reportsWhenTheCreatedCatalogItemCannotBeReloaded() {
        when(this.movieRepository.findById(MovieId.of(50L))).thenReturn(Mono.empty());

        StepVerifier.create(this.enricher.enrich(MovieId.of(50L), 123L))
                .expectError(MovieNotFoundException.class)
                .verify();
    }

    private static Movie movie() {
        return new Movie(
                MovieId.of(50L),
                "Javier",
                "Dune",
                MovieStatus.READY,
                EnrichmentStatus.RAW,
                null,
                null,
                MovieVisibility.PRIVATE,
                Set.of(),
                MediaKind.MOVIE);
    }
}
