package com.gcorp.service.app.mvflix_movies.catalog.application;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.gcorp.service.app.mvflix_movies.shared.application.security.AuthenticatedUser;
import com.gcorp.service.app.mvflix_movies.shared.application.security.UserProvider;
import com.gcorp.service.app.mvflix_movies.catalog.application.port.LibraryAssetLinks;
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

@ExtendWith(MockitoExtension.class)
class DeleteMovieUseCaseTest {

    @Mock private MovieRepository movieRepository;
    @Mock private LibraryAssetLinks libraryAssetLinks;
    @Mock private UserProvider userProvider;

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
        when(this.libraryAssetLinks.unlinkByMovieId(any())).thenReturn(Mono.empty());
        when(this.movieRepository.deleteById(MovieId.of(1L))).thenReturn(Mono.just(true));

        StepVerifier.create(this.useCase.execute(MovieId.of(1L))).verifyComplete();

        verify(this.movieRepository).deleteById(MovieId.of(1L));
    }

    @Test
    void adminCanDeleteMoviesOfOthers() {
        when(this.userProvider.getAuthenticatedUser())
                .thenReturn(Mono.just(new AuthenticatedUser("Admin", "a@m.com",
                        java.util.Set.of(AuthenticatedUser.ADMIN_ROLE))));
        when(this.movieRepository.findById(MovieId.of(1L))).thenReturn(Mono.just(movie(1L, "Javier")));
        when(this.libraryAssetLinks.unlinkByMovieId(any())).thenReturn(Mono.empty());
        when(this.movieRepository.deleteById(MovieId.of(1L))).thenReturn(Mono.just(true));

        StepVerifier.create(this.useCase.execute(MovieId.of(1L))).verifyComplete();
    }

    @Test
    void nonOwnerWithoutAdminRoleGetsNotFoundWithoutRevealingExistence() {
        when(this.userProvider.getAuthenticatedUser())
                .thenReturn(Mono.just(new AuthenticatedUser("Maria", "m@m.com")));
        when(this.movieRepository.findById(MovieId.of(1L))).thenReturn(Mono.just(movie(1L, "Javier")));

        StepVerifier.create(this.useCase.execute(MovieId.of(1L)))
                .expectError(MovieNotFoundException.class)
                .verify();

        verify(this.movieRepository, never()).deleteById(any());
        verify(this.libraryAssetLinks, never()).unlinkByMovieId(any());
    }
}
