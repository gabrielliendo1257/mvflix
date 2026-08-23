package com.gcorp.service.app.mvflix_movies.catalog.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.gcorp.service.app.mvflix_movies.app.security.AuthenticatedUser;
import com.gcorp.service.app.mvflix_movies.app.security.UserProvider;
import com.gcorp.service.app.mvflix_movies.catalog.application.port.LibraryMovieIds;
import com.gcorp.service.app.mvflix_movies.catalog.domain.movie.EnrichmentStatus;
import com.gcorp.service.app.mvflix_movies.catalog.domain.movie.MediaKind;
import com.gcorp.service.app.mvflix_movies.catalog.domain.movie.Movie;
import com.gcorp.service.app.mvflix_movies.catalog.domain.movie.MovieId;
import com.gcorp.service.app.mvflix_movies.catalog.domain.movie.MovieMetadata;
import com.gcorp.service.app.mvflix_movies.catalog.domain.movie.MovieRepository;
import com.gcorp.service.app.mvflix_movies.catalog.domain.movie.MovieStatus;
import com.gcorp.service.app.mvflix_movies.catalog.domain.movie.MovieVisibility;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.List;
import java.util.Set;

@ExtendWith(MockitoExtension.class)
class BulkVisibilityUseCaseTest {

    @Mock private MovieRepository movieRepository;
    @Mock private LibraryMovieIds libraryMovieIds;
    @Mock private UserProvider userProvider;

    @InjectMocks private BulkVisibilityUseCase useCase;

    @Test
    void sharedBulkPersistsOneAccessTransitionPerMovie() {
        Movie movie = movie(1L, MovieVisibility.PRIVATE, Set.of());
        when(this.userProvider.getAuthenticatedUser())
                .thenReturn(Mono.just(new AuthenticatedUser("Javier", "j@m.com")));
        when(this.movieRepository.findByOwnerAndIds("Javier", List.of(MovieId.of(1L))))
                .thenReturn(Flux.just(movie));
        when(this.movieRepository.updateAccess(any(Movie.class)))
                .thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        StepVerifier.create(this.useCase.execute(
                        List.of(MovieId.of(1L)), List.of(), MovieVisibility.SHARED,
                        List.of("Maria", "Pedro", "Maria", "  ")))
                .expectNext(new BulkVisibilityResult(1, 1, 0))
                .verifyComplete();

        ArgumentCaptor<Movie> captor = ArgumentCaptor.forClass(Movie.class);
        verify(this.movieRepository).updateAccess(captor.capture());
        assertThat(captor.getValue().getVisibility()).isEqualTo(MovieVisibility.SHARED);
        assertThat(captor.getValue().getSharedWith()).isEqualTo(Set.of("Maria", "Pedro"));
    }

    @Test
    void nonSharedBulkPreservesExistingSharesInAggregate() {
        Movie movie = movie(1L, MovieVisibility.SHARED, Set.of("Maria"));
        when(this.userProvider.getAuthenticatedUser())
                .thenReturn(Mono.just(new AuthenticatedUser("Javier", "j@m.com")));
        when(this.movieRepository.findByOwnerAndIds("Javier", List.of(MovieId.of(1L))))
                .thenReturn(Flux.just(movie));
        when(this.movieRepository.updateAccess(any(Movie.class)))
                .thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        StepVerifier.create(this.useCase.execute(
                        List.of(MovieId.of(1L)), List.of(), MovieVisibility.PRIVATE, List.of()))
                .expectNext(new BulkVisibilityResult(1, 1, 0))
                .verifyComplete();

        ArgumentCaptor<Movie> captor = ArgumentCaptor.forClass(Movie.class);
        verify(this.movieRepository).updateAccess(captor.capture());
        assertThat(captor.getValue().getVisibility()).isEqualTo(MovieVisibility.PRIVATE);
        assertThat(captor.getValue().getSharedWith()).containsExactly("Maria");
    }

    @Test
    void sharedBulkWithoutUsersFailsBeforeLoadingCatalog() {
        StepVerifier.create(this.useCase.execute(
                        List.of(MovieId.of(1L)), List.of(), MovieVisibility.SHARED,
                        List.of("  ")))
                .expectError(IllegalArgumentException.class)
                .verify();

        verify(this.movieRepository, never()).updateAccess(any(Movie.class));
        verify(this.userProvider, never()).getAuthenticatedUser();
    }

    @Test
    void resolvesLibraryMoviesThroughCatalogPort() {
        Movie movie = movie(2L, MovieVisibility.PRIVATE, Set.of());
        when(this.userProvider.getAuthenticatedUser())
                .thenReturn(Mono.just(new AuthenticatedUser("Javier", "j@m.com")));
        when(this.libraryMovieIds.findIdentifiedByLibraryIds(List.of(7L)))
                .thenReturn(Flux.just(MovieId.of(2L)));
        when(this.movieRepository.findByOwnerAndIds("Javier", List.of(MovieId.of(2L))))
                .thenReturn(Flux.just(movie));
        when(this.movieRepository.updateAccess(any(Movie.class)))
                .thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        StepVerifier.create(this.useCase.execute(
                        List.of(), List.of(7L), MovieVisibility.PUBLIC, List.of()))
                .expectNext(new BulkVisibilityResult(1, 1, 0))
                .verifyComplete();

        verify(this.libraryMovieIds).findIdentifiedByLibraryIds(List.of(7L));
    }

    private static Movie movie(long id, MovieVisibility visibility, Set<String> shares) {
        return new Movie(
                MovieId.of(id), "Javier", "Dune", MovieStatus.READY,
                EnrichmentStatus.RAW, null, MovieMetadata.onlyTitle("Dune"),
                visibility, shares, MediaKind.MOVIE);
    }
}
