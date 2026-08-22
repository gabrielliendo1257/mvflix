package com.gcorp.service.app.mvflix_movies.catalog.application;

import static org.mockito.Mockito.when;

import com.gcorp.service.app.mvflix_movies.catalog.domain.movie.EnrichmentStatus;
import com.gcorp.service.app.mvflix_movies.catalog.domain.movie.MediaKind;
import com.gcorp.service.app.mvflix_movies.catalog.domain.movie.Movie;
import com.gcorp.service.app.mvflix_movies.catalog.domain.movie.MovieAccessDeniedException;
import com.gcorp.service.app.mvflix_movies.catalog.domain.movie.MovieId;
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
class LibraryCatalogItemAccessTest {

    @Mock private MovieRepository movieRepository;

    @InjectMocks private LibraryCatalogItemAccess access;

    @Test
    void allowsAccessWhenCatalogItemIsVisibleToUser() {
        Movie movie = movie(MovieVisibility.SHARED, Set.of("Maria"));
        when(this.movieRepository.findById(MovieId.of(10L))).thenReturn(Mono.just(movie));

        StepVerifier.create(this.access.requireVisible(MovieId.of(10L), "Maria"))
                .verifyComplete();
    }

    @Test
    void deniesAccessWhenCatalogItemIsNotVisibleToUser() {
        Movie movie = movie(MovieVisibility.PRIVATE, Set.of());
        when(this.movieRepository.findById(MovieId.of(10L))).thenReturn(Mono.just(movie));

        StepVerifier.create(this.access.requireVisible(MovieId.of(10L), "Maria"))
                .expectError(MovieAccessDeniedException.class)
                .verify();
    }

    @Test
    void doesNotRevealWhetherCatalogItemExists() {
        when(this.movieRepository.findById(MovieId.of(10L))).thenReturn(Mono.empty());

        StepVerifier.create(this.access.requireVisible(MovieId.of(10L), "Maria"))
                .expectError(MovieAccessDeniedException.class)
                .verify();
    }

    private static Movie movie(MovieVisibility visibility, Set<String> sharedWith) {
        return new Movie(
                MovieId.of(10L),
                "Javier",
                "Dune",
                MovieStatus.READY,
                EnrichmentStatus.ENRICHED,
                null,
                null,
                visibility,
                sharedWith,
                MediaKind.MOVIE);
    }
}
