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

@ExtendWith(MockitoExtension.class)
class UpdateVisibilityUseCaseTest {

    @Mock private MovieRepository movieRepository;
    @Mock private UserProvider userProvider;

    @InjectMocks private UpdateVisibilityUseCase useCase;

    private static Movie movie(long id, String owner, MovieVisibility visibility) {
        return new Movie(
                MovieId.of(id), owner, "Dune", MovieStatus.READY, EnrichmentStatus.ENRICHED,
                null, null, visibility, java.util.Set.of(), MediaKind.MOVIE);
    }

    @Test
    void ownerChangesVisibility() {
        Movie movie = movie(1L, "Javier", MovieVisibility.PRIVATE);
        Movie published = movie(1L, "Javier", MovieVisibility.PUBLIC);

        when(this.userProvider.getAuthenticatedUser())
                .thenReturn(Mono.just(new AuthenticatedUser("Javier", "j@m.com")));
        when(this.movieRepository.findById(MovieId.of(1L)))
                .thenReturn(Mono.just(movie));
        when(this.movieRepository.updateVisibility(
                MovieId.of(1L), MovieVisibility.PUBLIC))
                .thenReturn(Mono.just(published));

        StepVerifier.create(this.useCase.execute(MovieId.of(1L), MovieVisibility.PUBLIC))
                .expectNextMatches(m -> m.getVisibility() == MovieVisibility.PUBLIC)
                .verifyComplete();

        verify(this.movieRepository).updateVisibility(
                MovieId.of(1L), MovieVisibility.PUBLIC);
    }

    @Test
    void nonOwnerIsDenied() {
        Movie movie = movie(1L, "Javier", MovieVisibility.PRIVATE);

        when(this.userProvider.getAuthenticatedUser())
                .thenReturn(Mono.just(new AuthenticatedUser("Maria", "m@m.com")));
        when(this.movieRepository.findById(MovieId.of(1L)))
                .thenReturn(Mono.just(movie));

        StepVerifier.create(this.useCase.execute(MovieId.of(1L), MovieVisibility.PUBLIC))
                .expectError(MovieAccessDeniedException.class)
                .verify();

        verify(this.movieRepository, never())
                .updateVisibility(eq(MovieId.of(1L)), any());
    }

    @Test
    void invisibleMovieIsDenied() {
        when(this.userProvider.getAuthenticatedUser())
                .thenReturn(Mono.just(new AuthenticatedUser("Maria", "m@m.com")));
        when(this.movieRepository.findById(MovieId.of(1L)))
                .thenReturn(Mono.empty());

        StepVerifier.create(this.useCase.execute(MovieId.of(1L), MovieVisibility.PUBLIC))
                .expectError(MovieAccessDeniedException.class)
                .verify();

        verify(this.movieRepository, never())
                .updateVisibility(eq(MovieId.of(1L)), any());
    }
}