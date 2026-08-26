package com.gcorp.service.app.mvflix_movies.catalog.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
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

import org.junit.jupiter.api.BeforeEach;
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
class UpdateMovieAccessUseCaseTest {

    @Mock private MovieRepository movieRepository;
    @Mock private UserProvider userProvider;

    @InjectMocks private UpdateMovieAccessUseCase useCase;

    private Movie movie;

    @BeforeEach
    void setUp() {
        this.movie = new Movie(
                MovieId.of(1L), "Javier", "Dune", MovieStatus.READY,
                EnrichmentStatus.ENRICHED, null, null,
                MovieVisibility.PRIVATE, java.util.Set.of(), MediaKind.MOVIE);
        // Lenientes: el test de autorización corta el flujo antes de
        // consumir la cadena completa de stubs.
        org.mockito.Mockito.lenient()
                .when(this.userProvider.getAuthenticatedUser())
                .thenReturn(Mono.just(new AuthenticatedUser("Javier", "j@m.com")));
        org.mockito.Mockito.lenient()
                .when(this.movieRepository.findById(MovieId.of(1L)))
                .thenReturn(Mono.just(this.movie));
        org.mockito.Mockito.lenient()
                .when(this.movieRepository.updateAccess(any()))
                .thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));
    }

    @Test
    void appliesVisibilityAndSharesAsOneDecision() {
        StepVerifier.create(this.useCase.execute(
                        MovieId.of(1L), MovieVisibility.SHARED, List.of("Maria", "", "Pedro")))
                .assertNext(updated -> {
                    assertThat(updated.getVisibility()).isEqualTo(MovieVisibility.SHARED);
                    assertThat(updated.getSharedWith()).containsExactlyInAnyOrder("Maria", "Pedro");
                })
                .verifyComplete();

        ArgumentCaptor<Movie> captor = ArgumentCaptor.forClass(Movie.class);
        verify(this.movieRepository).updateAccess(captor.capture());
        assertThat(captor.getValue().getSharedWith()).containsExactlyInAnyOrder("Maria", "Pedro");
    }

    @Test
    void blankAndDuplicateUsernamesAreNormalized() {
        StepVerifier.create(this.useCase.execute(
                        MovieId.of(1L), MovieVisibility.SHARED, List.of("Maria", "Maria", "  ")))
                .assertNext(updated ->
                        assertThat(updated.getSharedWith()).containsExactly("Maria"))
                .verifyComplete();
    }

    @Test
    void nonOwnerIsDeniedWithoutPersisting() {
        org.mockito.Mockito.lenient()
                .when(this.userProvider.getAuthenticatedUser())
                .thenReturn(Mono.just(new AuthenticatedUser("Maria", "m@m.com")));
        // findById/updateAccess quedan sin stub estricto: el flujo se corta
        // en la autorización, así que no se consumen.
        org.mockito.Mockito.lenient()
                .when(this.movieRepository.findById(org.mockito.ArgumentMatchers.any()))
                .thenReturn(Mono.empty());

        StepVerifier.create(this.useCase.execute(
                        MovieId.of(1L), MovieVisibility.PUBLIC, List.of()))
                .expectError(MovieAccessDeniedException.class)
                .verify();

        verify(this.movieRepository, org.mockito.Mockito.never()).updateAccess(any());
    }
}
