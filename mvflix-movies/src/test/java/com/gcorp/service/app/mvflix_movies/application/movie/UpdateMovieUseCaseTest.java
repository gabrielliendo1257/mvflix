package com.gcorp.service.app.mvflix_movies.application.movie;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.gcorp.service.app.mvflix_movies.app.security.AuthenticatedUser;
import com.gcorp.service.app.mvflix_movies.app.security.UserProvider;
import com.gcorp.service.app.mvflix_movies.domain.movie.EnrichmentStatus;
import com.gcorp.service.app.mvflix_movies.domain.movie.Movie;
import com.gcorp.service.app.mvflix_movies.domain.movie.MovieAccessDeniedException;
import com.gcorp.service.app.mvflix_movies.domain.movie.MovieId;
import com.gcorp.service.app.mvflix_movies.domain.movie.MovieMetadata;
import com.gcorp.service.app.mvflix_movies.domain.movie.MovieRepository;
import com.gcorp.service.app.mvflix_movies.domain.movie.MovieStatus;
import com.gcorp.service.app.mvflix_movies.domain.movie.MovieVisibility;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.List;

@ExtendWith(MockitoExtension.class)
class UpdateMovieUseCaseTest {

    @Mock private MovieRepository movieRepository;
    @Mock private UserProvider userProvider;

    @InjectMocks private UpdateMovieUseCase useCase;

    private static final MovieMetadata METADATA = new MovieMetadata(
            "Dune", "Dune", 2021, List.of("Sci-Fi"), 7.9, "2h 35m", "Denis Villeneuve",
            List.of("Timothée Chalamet"), "Overview", "/poster.jpg", "2021-10-22",
            "USA", "English", List.of("Oscar"), 438631L);

    private static Movie movie(long id, String owner, MovieMetadata metadata) {
        return new Movie(
                MovieId.of(id), owner, "Dune", MovieStatus.READY, EnrichmentStatus.ENRICHED,
                null, metadata, MovieVisibility.PRIVATE, java.util.Set.of());
    }

    @Test
    void ownerUpdatesProvidedFieldsAndKeepsOthers() {
        Movie movie = movie(1L, "Javier", METADATA);
        Movie updated = movie(1L, "Javier", new MovieMetadata(
                "Dune: Part Two", "Dune: Part Two", 2024, List.of("Sci-Fi", "Adventure"),
                7.9, "2h 35m", "Denis Villeneuve", List.of("Timothée Chalamet"),
                "Overview", "/poster.jpg", "2024-03-01", "USA", "English",
                List.of("Oscar"), 438631L));

        when(this.userProvider.getAuthenticatedUser())
                .thenReturn(Mono.just(new AuthenticatedUser("Javier", "j@m.com")));
        when(this.movieRepository.findById(MovieId.of(1L))).thenReturn(Mono.just(movie));
        when(this.movieRepository.updateMetadata(eq(MovieId.of(1L)), any()))
                .thenReturn(Mono.just(updated));

        var command = new UpdateMovieCommand(
                "Dune: Part Two", "Dune: Part Two", 2024, List.of("Sci-Fi", "Adventure"),
                null, null, null, null, "2024-03-01", null, null, null);

        StepVerifier.create(this.useCase.execute(MovieId.of(1L), command))
                .expectNext(updated)
                .verifyComplete();

        ArgumentCaptor<MovieMetadata> captor = ArgumentCaptor.forClass(MovieMetadata.class);
        verify(this.movieRepository).updateMetadata(eq(MovieId.of(1L)), captor.capture());
        assertThat(captor.getValue().title()).isEqualTo("Dune: Part Two");
        assertThat(captor.getValue().year()).isEqualTo(2024);
        assertThat(captor.getValue().genres()).containsExactly("Sci-Fi", "Adventure");
        assertThat(captor.getValue().releaseDate()).isEqualTo("2024-03-01");
        assertThat(captor.getValue().duration()).isEqualTo("2h 35m");
        assertThat(captor.getValue().posterPath()).isEqualTo("/poster.jpg");
        assertThat(captor.getValue().tmdbId()).isEqualTo(438631L);
    }

    @Test
    void mergeKeepsNullFieldsAndNonEditableIdentity() {
        MovieMetadata merged = UpdateMovieUseCase.merge(METADATA, new UpdateMovieCommand(
                "Dune: Part Two", null, null, List.of(), null, null, null, "Nueva sinopsis",
                null, null, null, null));

        assertThat(merged.title()).isEqualTo("Dune: Part Two");
        assertThat(merged.genres()).isEmpty();
        assertThat(merged.overview()).isEqualTo("Nueva sinopsis");
        assertThat(merged.popularity()).isEqualTo(7.9);
        assertThat(merged.posterPath()).isEqualTo("/poster.jpg");
        assertThat(merged.tmdbId()).isEqualTo(438631L);
        assertThat(merged.year()).isEqualTo(2021);
    }

    @Test
    void nonOwnerIsDenied() {
        Movie movie = movie(1L, "Javier", METADATA);

        when(this.userProvider.getAuthenticatedUser())
                .thenReturn(Mono.just(new AuthenticatedUser("Maria", "m@m.com")));
        when(this.movieRepository.findById(MovieId.of(1L))).thenReturn(Mono.just(movie));

        StepVerifier.create(this.useCase.execute(MovieId.of(1L), new UpdateMovieCommand(
                        "Otro", null, null, null, null, null, null, null,
                        null, null, null, null)))
                .expectError(MovieAccessDeniedException.class)
                .verify();

        verify(this.movieRepository, never()).updateMetadata(eq(MovieId.of(1L)), any());
    }

    @Test
    void missingMovieIsDeniedWithoutRevealingExistence() {
        when(this.userProvider.getAuthenticatedUser())
                .thenReturn(Mono.just(new AuthenticatedUser("Javier", "j@m.com")));
        when(this.movieRepository.findById(MovieId.of(99L))).thenReturn(Mono.empty());

        StepVerifier.create(this.useCase.execute(MovieId.of(99L), new UpdateMovieCommand(
                        "X", null, null, null, null, null, null, null,
                        null, null, null, null)))
                .expectError(MovieAccessDeniedException.class)
                .verify();

        verify(this.movieRepository, never()).updateMetadata(eq(MovieId.of(99L)), any());
    }
}