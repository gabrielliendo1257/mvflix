package com.gcorp.service.app.mvflix_movies.catalog.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.gcorp.service.app.mvflix_movies.shared.application.security.AuthenticatedUser;
import com.gcorp.service.app.mvflix_movies.shared.application.security.UserProvider;
import com.gcorp.service.app.mvflix_movies.catalog.domain.movie.EnrichmentStatus;
import com.gcorp.service.app.mvflix_movies.catalog.domain.movie.MediaKind;
import com.gcorp.service.app.mvflix_movies.catalog.domain.movie.Movie;
import com.gcorp.service.app.mvflix_movies.catalog.domain.movie.MovieAccessDeniedException;
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
                null, metadata, MovieVisibility.PRIVATE, java.util.Set.of(), MediaKind.MOVIE);
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
        when(this.movieRepository.updateDetails(any(Movie.class)))
                .thenReturn(Mono.just(updated));

        var command = new UpdateMovieCommand(
                "Dune: Part Two", "Dune: Part Two", 2024, List.of("Sci-Fi", "Adventure"),
                null, null, null, null, null, "2024-03-01", null, null, null, null, null);

        StepVerifier.create(this.useCase.execute(MovieId.of(1L), command))
                .expectNext(updated)
                .verifyComplete();

        ArgumentCaptor<Movie> captor = ArgumentCaptor.forClass(Movie.class);
        verify(this.movieRepository).updateDetails(captor.capture());
        assertThat(captor.getValue().getId()).isEqualTo(MovieId.of(1L));
        assertThat(captor.getValue().getMetadata().title()).isEqualTo("Dune: Part Two");
        assertThat(captor.getValue().getMetadata().year()).isEqualTo(2024);
        assertThat(captor.getValue().getMetadata().genres())
                .containsExactly("Sci-Fi", "Adventure");
        assertThat(captor.getValue().getMetadata().releaseDate()).isEqualTo("2024-03-01");
        assertThat(captor.getValue().getMetadata().duration()).isEqualTo("2h 35m");
        assertThat(captor.getValue().getMetadata().posterPath()).isEqualTo("/poster.jpg");
        assertThat(captor.getValue().getMetadata().tmdbId()).isEqualTo(438631L);
    }

    @Test
    void mergeKeepsNullFieldsAndNonEditableIdentity() {
        MovieMetadata merged = UpdateMovieUseCase.merge(METADATA, new UpdateMovieCommand(
                "Dune: Part Two", null, null, List.of(), null, null, null, "Nueva sinopsis",
                null, null, null, null, null, null, null));

        assertThat(merged.title()).isEqualTo("Dune: Part Two");
        assertThat(merged.genres()).isEmpty();
        assertThat(merged.overview()).isEqualTo("Nueva sinopsis");
        assertThat(merged.popularity()).isEqualTo(7.9);
        assertThat(merged.posterPath()).isEqualTo("/poster.jpg");
        assertThat(merged.tmdbId()).isEqualTo(438631L);
        assertThat(merged.year()).isEqualTo(2021);
    }

    @Test
    void mergeUpdatesPosterAndPopularity() {
        MovieMetadata merged = UpdateMovieUseCase.merge(METADATA, new UpdateMovieCommand(
                null, null, null, null, null, null, null, null,
                "/nuevo-poster.jpg", null, null, null, null, 9.1, null));

        assertThat(merged.posterPath()).isEqualTo("/nuevo-poster.jpg");
        assertThat(merged.popularity()).isEqualTo(9.1);
        assertThat(merged.tmdbId()).isEqualTo(438631L);
        assertThat(merged.title()).isEqualTo("Dune");
    }

    @Test
    void switchingToOtherClearsProviderMetadata() {
        Movie movie = movie(1L, "Javier", METADATA);

        when(this.userProvider.getAuthenticatedUser())
                .thenReturn(Mono.just(new AuthenticatedUser("Javier", "j@m.com")));
        when(this.movieRepository.findById(MovieId.of(1L))).thenReturn(Mono.just(movie));
        when(this.movieRepository.updateDetails(any(Movie.class)))
                .thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        StepVerifier.create(this.useCase.execute(MovieId.of(1L), new UpdateMovieCommand(
                        "Mi grabacion", null, null, null, null, null, null, null,
                        null, null, null, null, null, null, MediaKind.VIDEO)))
                .expectNextCount(1)
                .verifyComplete();

        ArgumentCaptor<Movie> captor = ArgumentCaptor.forClass(Movie.class);
        verify(this.movieRepository).updateDetails(captor.capture());
        Movie reclassified = captor.getValue();
        assertThat(reclassified.getKind()).isEqualTo(MediaKind.VIDEO);
        assertThat(reclassified.getEnrichmentStatus()).isEqualTo(EnrichmentStatus.RAW);
        assertThat(reclassified.getMetadata().title()).isEqualTo("Mi grabacion");
        assertThat(reclassified.getMetadata().tmdbId()).isNull();
        assertThat(reclassified.getMetadata().posterPath()).isNull();
        assertThat(reclassified.getMetadata().popularity()).isNull();
        assertThat(reclassified.getMetadata().year()).isNull();
        assertThat(reclassified.getMetadata().overview()).isNull();
    }

    @Test
    void switchingFromOtherCreatesRawUnlinkedMovie() {
        Movie other = new Movie(
                MovieId.of(1L), "Javier", "Imported clip", MovieStatus.READY,
                EnrichmentStatus.RAW, null, MovieMetadata.onlyTitle("Imported clip"),
                MovieVisibility.PRIVATE, java.util.Set.of(), MediaKind.VIDEO);

        when(this.userProvider.getAuthenticatedUser())
                .thenReturn(Mono.just(new AuthenticatedUser("Javier", "j@m.com")));
        when(this.movieRepository.findById(MovieId.of(1L))).thenReturn(Mono.just(other));
        when(this.movieRepository.updateDetails(any(Movie.class)))
                .thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        StepVerifier.create(this.useCase.execute(MovieId.of(1L), new UpdateMovieCommand(
                        "Identifiable movie", null, null, null, null, null, null, null,
                        null, null, null, null, null, null, MediaKind.MOVIE)))
                .assertNext(updated -> {
                    assertThat(updated.getKind()).isEqualTo(MediaKind.MOVIE);
                    assertThat(updated.getEnrichmentStatus()).isEqualTo(EnrichmentStatus.RAW);
                    assertThat(updated.getMetadata().title()).isEqualTo("Identifiable movie");
                    assertThat(updated.getMetadata().tmdbId()).isNull();
                })
                .verifyComplete();

        verify(this.movieRepository).updateDetails(any(Movie.class));
    }

    @Test
    void nonOwnerIsDenied() {
        Movie movie = movie(1L, "Javier", METADATA);

        when(this.userProvider.getAuthenticatedUser())
                .thenReturn(Mono.just(new AuthenticatedUser("Maria", "m@m.com")));
        when(this.movieRepository.findById(MovieId.of(1L))).thenReturn(Mono.just(movie));

        StepVerifier.create(this.useCase.execute(MovieId.of(1L), new UpdateMovieCommand(
                        "Otro", null, null, null, null, null, null, null,
                        null, null, null, null, null, null, null)))
                .expectError(MovieAccessDeniedException.class)
                .verify();

        verify(this.movieRepository, never()).updateDetails(any(Movie.class));
    }

    @Test
    void adminCanModerateMoviesOfOthers() {
        Movie movie = movie(1L, "Javier", METADATA);

        when(this.userProvider.getAuthenticatedUser())
                .thenReturn(Mono.just(new AuthenticatedUser("Admin", "a@m.com",
                        java.util.Set.of(AuthenticatedUser.ADMIN_ROLE))));
        when(this.movieRepository.findById(MovieId.of(1L))).thenReturn(Mono.just(movie));
        when(this.movieRepository.updateDetails(any(Movie.class)))
                .thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        StepVerifier.create(this.useCase.execute(MovieId.of(1L), new UpdateMovieCommand(
                        "Moderado", null, null, null, null, null, null, null,
                        null, null, null, null, null, null, null)))
                .expectNextCount(1)
                .verifyComplete();

        verify(this.movieRepository).updateDetails(any(Movie.class));
    }

    @Test
    void adminWithoutRoleAuthorityIsJustAnotherNonOwner() {
        Movie movie = movie(1L, "Javier", METADATA);

        // Username "Admin" sin el rol: la política la decide el token, no el nombre.
        when(this.userProvider.getAuthenticatedUser())
                .thenReturn(Mono.just(new AuthenticatedUser("Admin", "a@m.com")));
        when(this.movieRepository.findById(MovieId.of(1L))).thenReturn(Mono.just(movie));

        StepVerifier.create(this.useCase.execute(MovieId.of(1L), new UpdateMovieCommand(
                        "X", null, null, null, null, null, null, null,
                        null, null, null, null, null, null, null)))
                .expectError(MovieAccessDeniedException.class)
                .verify();
    }

    @Test
    void missingMovieIsDeniedWithoutRevealingExistence() {
        when(this.userProvider.getAuthenticatedUser())
                .thenReturn(Mono.just(new AuthenticatedUser("Javier", "j@m.com")));
        when(this.movieRepository.findById(MovieId.of(99L))).thenReturn(Mono.empty());

        StepVerifier.create(this.useCase.execute(MovieId.of(99L), new UpdateMovieCommand(
                        "X", null, null, null, null, null, null, null,
                        null, null, null, null, null, null, null)))
                .expectError(MovieAccessDeniedException.class)
                .verify();

        verify(this.movieRepository, never()).updateDetails(any(Movie.class));
    }
}
