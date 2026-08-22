package com.gcorp.service.app.mvflix_movies.application.movie;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.gcorp.service.app.mvflix_movies.app.security.AuthenticatedUser;
import com.gcorp.service.app.mvflix_movies.app.security.UserProvider;
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

import java.util.List;

@ExtendWith(MockitoExtension.class)
class UpdateSharesUseCaseTest {

    @Mock private MovieRepository movieRepository;
    @Mock private UserProvider userProvider;

    @InjectMocks private UpdateSharesUseCase useCase;

    private static Movie movie(long id, String owner) {
        return new Movie(
                MovieId.of(id), owner, "Dune", MovieStatus.READY, EnrichmentStatus.ENRICHED,
                null, null, MovieVisibility.SHARED, java.util.Set.of(), MediaKind.MOVIE);
    }

    @Test
    void ownerReplacesShares() {
        Movie movie = movie(1L, "Javier");

        when(this.userProvider.getAuthenticatedUser())
                .thenReturn(Mono.just(new AuthenticatedUser("Javier", "j@m.com")));
        when(this.movieRepository.findById(MovieId.of(1L)))
                .thenReturn(Mono.just(movie));
        when(this.movieRepository.replaceShares(MovieId.of(1L), List.of("Maria", "Pedro")))
                .thenReturn(Mono.just(movie));

        StepVerifier.create(this.useCase.execute(
                        MovieId.of(1L), List.of("Maria", "Pedro", "Maria", "  ")))
                .expectNextCount(1)
                .verifyComplete();

        verify(this.movieRepository).replaceShares(
                MovieId.of(1L), List.of("Maria", "Pedro"));
    }

    @Test
    void nonOwnerIsDenied() {
        Movie movie = movie(1L, "Javier");

        when(this.userProvider.getAuthenticatedUser())
                .thenReturn(Mono.just(new AuthenticatedUser("Maria", "m@m.com")));
        when(this.movieRepository.findById(MovieId.of(1L)))
                .thenReturn(Mono.just(movie));

        StepVerifier.create(this.useCase.execute(MovieId.of(1L), List.of("Javier")))
                .expectError(MovieAccessDeniedException.class)
                .verify();

        verify(this.movieRepository, never()).replaceShares(eq(MovieId.of(1L)), any());
    }
}