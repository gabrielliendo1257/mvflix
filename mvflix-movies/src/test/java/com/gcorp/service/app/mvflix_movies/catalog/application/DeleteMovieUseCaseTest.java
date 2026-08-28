package com.gcorp.service.app.mvflix_movies.catalog.application;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.gcorp.service.app.mvflix_movies.shared.application.security.AuthenticatedUser;
import com.gcorp.service.app.mvflix_movies.shared.application.security.UserProvider;
import com.gcorp.service.app.mvflix_movies.catalog.domain.media.MediaRepository;
import com.gcorp.service.app.mvflix_movies.catalog.domain.media.Media;
import com.gcorp.service.app.mvflix_movies.catalog.domain.media.MediaId;
import com.gcorp.service.app.mvflix_movies.catalog.domain.movie.EnrichmentStatus;
import com.gcorp.service.app.mvflix_movies.catalog.domain.movie.MediaKind;
import com.gcorp.service.app.mvflix_movies.catalog.domain.movie.Movie;
import com.gcorp.service.app.mvflix_movies.catalog.domain.movie.MovieId;
import com.gcorp.service.app.mvflix_movies.catalog.domain.movie.MovieMetadata;
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

import java.time.Instant;

@ExtendWith(MockitoExtension.class)
class DeleteMovieUseCaseTest {

    @Mock private MovieRepository movieRepository;
    @Mock private MediaRepository mediaRepository;
    @Mock private UserProvider userProvider;
    @Mock private MovieDeletionTransaction deletionTransaction;

    @InjectMocks private DeleteMovieUseCase useCase;

    private static Movie movie(long id, String owner) {
        return new Movie(
                MovieId.of(id), owner, "Dune", MovieStatus.READY, EnrichmentStatus.ENRICHED,
                77L, null, MovieVisibility.PRIVATE, java.util.Set.of(), MediaKind.MOVIE);
    }

    @Test
    void ownerDeletesOwnMovie() {
        when(this.userProvider.getAuthenticatedUser())
                .thenReturn(Mono.just(new AuthenticatedUser("Javier", "j@m.com")));
        when(this.movieRepository.findById(MovieId.of(1L))).thenReturn(Mono.just(movie(1L, "Javier")));
        when(this.mediaRepository.findByMovieId(MovieId.of(1L))).thenReturn(Mono.empty());
        when(this.deletionTransaction.deleteImmediately(MovieId.of(1L))).thenReturn(Mono.empty());

        StepVerifier.create(this.useCase.execute(MovieId.of(1L)))
                .expectNext(new DeletionOutcome.Completed())
                .verifyComplete();

        verify(this.deletionTransaction).deleteImmediately(MovieId.of(1L));
    }

    @Test
    void adminCanDeleteMoviesOfOthers() {
        when(this.userProvider.getAuthenticatedUser())
                .thenReturn(Mono.just(new AuthenticatedUser("Admin", "a@m.com",
                        java.util.Set.of(AuthenticatedUser.ADMIN_ROLE))));
        when(this.movieRepository.findById(MovieId.of(1L))).thenReturn(Mono.just(movie(1L, "Javier")));
        when(this.mediaRepository.findByMovieId(MovieId.of(1L))).thenReturn(Mono.empty());
        when(this.deletionTransaction.deleteImmediately(MovieId.of(1L))).thenReturn(Mono.empty());

        StepVerifier.create(this.useCase.execute(MovieId.of(1L)))
                .expectNext(new DeletionOutcome.Completed())
                .verifyComplete();
    }

    @Test
    void nonOwnerWithoutAdminRoleGetsNotFoundWithoutRevealingExistence() {
        when(this.userProvider.getAuthenticatedUser())
                .thenReturn(Mono.just(new AuthenticatedUser("Maria", "m@m.com")));
        when(this.movieRepository.findById(MovieId.of(1L))).thenReturn(Mono.just(movie(1L, "Javier")));

        StepVerifier.create(this.useCase.execute(MovieId.of(1L)))
                .expectNext(new DeletionOutcome.Completed())
                .verifyComplete();

        verify(this.deletionTransaction, never()).deleteImmediately(any());
    }

    @Test
    void managedMovieIsMarkedDeletingAndRequestedDurably() {
        Movie movie = movie(1L, "Javier");
        when(this.userProvider.getAuthenticatedUser())
                .thenReturn(Mono.just(new AuthenticatedUser("Javier", "j@m.com")));
        when(this.movieRepository.findById(MovieId.of(1L))).thenReturn(Mono.just(movie));
        when(this.mediaRepository.findByMovieId(MovieId.of(1L)))
                .thenReturn(Mono.just(new Media(MediaId.of(9L), MovieId.of(1L), 77L, "k", Instant.now())));
        when(this.deletionTransaction.requestDeletion(MovieId.of(1L)))
                .thenReturn(Mono.just(movie));

        StepVerifier.create(this.useCase.execute(MovieId.of(1L)))
                .expectNext(new DeletionOutcome.Pending())
                .verifyComplete();

        verify(this.deletionTransaction).requestDeletion(MovieId.of(1L));
    }

    @Test
    void durableDeletionRequestFailureIsPropagated() {
        Movie movie = movie(1L, "Javier");
        when(this.userProvider.getAuthenticatedUser())
                .thenReturn(Mono.just(new AuthenticatedUser("Javier", "j@m.com")));
        when(this.movieRepository.findById(MovieId.of(1L))).thenReturn(Mono.just(movie));
        when(this.mediaRepository.findByMovieId(MovieId.of(1L)))
                .thenReturn(Mono.just(new Media(MediaId.of(9L), MovieId.of(1L), 77L, "k", Instant.now())));
        RuntimeException failure = new RuntimeException("outbox unavailable");
        when(this.deletionTransaction.requestDeletion(MovieId.of(1L)))
                .thenReturn(Mono.error(failure));

        StepVerifier.create(this.useCase.execute(MovieId.of(1L)))
                .expectError(RuntimeException.class)
                .verify();
    }

    @Test
    void managedDeletionAlwaysUsesOutbox() {
        Movie movie = movie(1L, "Javier");
        when(this.userProvider.getAuthenticatedUser())
                .thenReturn(Mono.just(new AuthenticatedUser("Javier", "j@m.com")));
        when(this.movieRepository.findById(MovieId.of(1L))).thenReturn(Mono.just(movie));
        when(this.mediaRepository.findByMovieId(MovieId.of(1L)))
                .thenReturn(Mono.just(new Media(MediaId.of(9L), MovieId.of(1L), 77L, "k", Instant.now())));
        when(this.deletionTransaction.requestDeletion(MovieId.of(1L))).thenReturn(Mono.just(movie));

        StepVerifier.create(this.useCase.execute(MovieId.of(1L)))
                .expectNext(new DeletionOutcome.Pending())
                .verifyComplete();

        verify(this.deletionTransaction).requestDeletion(MovieId.of(1L));
    }

    @Test
    void alreadyDeletingMovieEnsuresDurableRequest() {
        Movie movie = new Movie(
                MovieId.of(1L), "Javier", "Dune", MovieStatus.DELETING, EnrichmentStatus.ENRICHED,
                77L, null, MovieVisibility.PRIVATE, java.util.Set.of(), MediaKind.MOVIE);
        when(this.userProvider.getAuthenticatedUser())
                .thenReturn(Mono.just(new AuthenticatedUser("Javier", "j@m.com")));
        when(this.movieRepository.findById(MovieId.of(1L))).thenReturn(Mono.just(movie));
        when(this.deletionTransaction.ensureDeletionRequested(MovieId.of(1L)))
                .thenReturn(Mono.empty());

        StepVerifier.create(this.useCase.execute(MovieId.of(1L)))
                .expectNext(new DeletionOutcome.Pending())
                .verifyComplete();

        verify(this.deletionTransaction).ensureDeletionRequested(MovieId.of(1L));
    }
}
