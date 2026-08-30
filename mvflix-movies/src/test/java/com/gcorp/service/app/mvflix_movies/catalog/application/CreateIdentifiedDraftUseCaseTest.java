package com.gcorp.service.app.mvflix_movies.catalog.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.gcorp.service.app.mvflix_movies.catalog.domain.movie.MediaKind;
import com.gcorp.service.app.mvflix_movies.catalog.domain.movie.Movie;
import com.gcorp.service.app.mvflix_movies.catalog.domain.movie.MovieMetadata;
import com.gcorp.service.app.mvflix_movies.catalog.domain.movie.MovieRepository;
import com.gcorp.service.app.mvflix_movies.catalog.domain.movie.MovieVisibility;
import com.gcorp.service.app.mvflix_movies.shared.application.security.AuthenticatedUser;
import com.gcorp.service.app.mvflix_movies.shared.application.security.UserProvider;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.List;
import java.util.List;

@ExtendWith(MockitoExtension.class)
class CreateIdentifiedDraftUseCaseTest {

  @Mock private MovieRepository movieRepository;
  @Mock private UserProvider userProvider;

  private CreateIdentifiedDraftUseCase useCase;

  @BeforeEach
  void setUp() {
    this.useCase = new CreateIdentifiedDraftUseCase(this.movieRepository, this.userProvider);
    org.mockito.Mockito.lenient()
        .when(this.userProvider.getAuthenticatedUser())
        .thenReturn(Mono.just(new AuthenticatedUser("pepe", "pepe@test")));
    // Simula el INSERT ... RETURNING: la fila guardada sale CON id asignado.
    org.mockito.Mockito.lenient()
        .when(this.movieRepository.saveDraftWithAccess(any(Movie.class)))
        .thenAnswer(invocation -> {
          Movie in = invocation.getArgument(0);
          return Mono.just(new Movie(
              com.gcorp.service.app.mvflix_movies.catalog.domain.movie.MovieId.of(1L),
              in.getOwnerUsername(),
              in.getTitle(),
              in.getStatus(),
              in.getEnrichmentStatus(),
              in.getObjectId(),
              in.getMetadata(),
              in.getVisibility(),
              in.getSharedWith(),
              in.getKind()));
        });
  }

  private MovieMetadata alienMetadata(Long tmdbId) {
    return new MovieMetadata(
        "Alien", "Alien", 1979, List.of("Horror"), null, "1h 57m",
        "Ridley Scott", List.of(), "Overview...", "poster.png",
        "1979-05-25", "UK", "English", List.of(), tmdbId);
  }

  private CreateIdentifiedDraftCommand command(
      MovieMetadata metadata, MediaKind kind,
      MovieVisibility visibility, List<String> shared) {
    return new CreateIdentifiedDraftCommand(metadata, kind, visibility, shared);
  }

  @Test
  void createsEnrichedDraftWithPrivateDefaultAccess() {
    StepVerifier.create(
            this.useCase.execute(command(
                alienMetadata(348L), MediaKind.MOVIE, null, List.of())))
        .assertNext(movie -> {
          assertThat(movie.isDraft()).isTrue();
          assertThat(movie.getEnrichmentStatus().name()).isEqualTo("ENRICHED");
          assertThat(movie.getMetadata().tmdbId()).isEqualTo(348L);
          assertThat(movie.getVisibility().name()).isEqualTo("PRIVATE");
          assertThat(movie.getSharedWith()).isEmpty();
        })
        .verifyComplete();

    verifyNoAccessFallback();
    verifySavedThroughAtomicPort();
  }

  @Test
  void sharedRequiresAtLeastOneUsername() {
    StepVerifier.create(
            this.useCase.execute(command(
                alienMetadata(348L), MediaKind.MOVIE,
                MovieVisibility.SHARED, List.of())))
        .expectError(IllegalArgumentException.class)
        .verify();

    verifyNoInteractions(this.movieRepository);
  }

  @Test
  void sharedCleansBlankAndDuplicateUsernames() {
    StepVerifier.create(
            this.useCase.execute(command(
                alienMetadata(348L), MediaKind.MOVIE,
                MovieVisibility.SHARED, List.of("ana", "ana", "  ", "luis"))))
        .assertNext(movie -> {
          assertThat(movie.getVisibility().name()).isEqualTo("SHARED");
          assertThat(movie.getSharedWith()).containsExactlyInAnyOrder("ana", "luis");
        })
        .verifyComplete();
  }

  @Test
  void movieWithoutTmdbIdIsRejectedBeforePersistence() {
    StepVerifier.create(
            this.useCase.execute(command(
                alienMetadata(null), MediaKind.MOVIE, MovieVisibility.PRIVATE, List.of())))
        .expectError(IllegalArgumentException.class)
        .verify();

    verifyNoInteractions(this.movieRepository);
  }

  @Test
  void otherKindStaysRawEvenWithProviderId() {
    StepVerifier.create(
            this.useCase.execute(command(
                alienMetadata(348L), MediaKind.VIDEO, MovieVisibility.PUBLIC, List.of())))
        .assertNext(movie -> {
          assertThat(movie.getEnrichmentStatus().name()).isEqualTo("RAW");
          assertThat(movie.getVisibility().name()).isEqualTo("PUBLIC");
        })
        .verifyComplete();
  }

  private void verifyNoAccessFallback() {
    // El acceso inicial NO se aplica con updates posteriores: es parte del save.
    verifyNoInteractionsFallbackPorts();
  }

  private void verifyNoInteractionsFallbackPorts() {
    org.mockito.Mockito.verify(this.movieRepository,
        org.mockito.Mockito.never()).updateAccess(any(Movie.class));
    org.mockito.Mockito.verify(this.movieRepository,
        org.mockito.Mockito.never()).replaceShares(any(Movie.class));
    org.mockito.Mockito.verify(this.movieRepository,
        org.mockito.Mockito.never()).updateVisibility(any(Movie.class));
  }

  private void verifySavedThroughAtomicPort() {
    org.mockito.Mockito.verify(this.movieRepository).saveDraftWithAccess(any(Movie.class));
  }
}
