package com.gcorp.service.app.mvflix_movies.catalog.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.gcorp.service.app.mvflix_movies.catalog.domain.movie.MediaKind;
import com.gcorp.service.app.mvflix_movies.catalog.domain.movie.CatalogItem;
import com.gcorp.service.app.mvflix_movies.catalog.domain.movie.MovieMetadata;
import com.gcorp.service.app.mvflix_movies.catalog.domain.movie.CatalogItemRepository;
import com.gcorp.service.app.mvflix_movies.catalog.domain.movie.CatalogItemVisibility;
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

  @Mock private CatalogItemRepository movieRepository;
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
        .when(this.movieRepository.saveDraftWithAccess(any(CatalogItem.class)))
        .thenAnswer(invocation -> {
          CatalogItem in = invocation.getArgument(0);
          return Mono.just(new CatalogItem(
              com.gcorp.service.app.mvflix_movies.catalog.domain.movie.CatalogItemId.of(1L),
              in.getOwnerUsername(),
              in.getTitle(),
              in.getStatus(),
              in.getEnrichmentStatus(),
              null,
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
      CatalogItemVisibility visibility, List<String> shared) {
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
          assertThat(movie.getMovieMetadata().tmdbId()).isEqualTo(348L);
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
                CatalogItemVisibility.SHARED, List.of())))
        .expectError(IllegalArgumentException.class)
        .verify();

    verifyNoInteractions(this.movieRepository);
  }

  @Test
  void sharedCleansBlankAndDuplicateUsernames() {
    StepVerifier.create(
            this.useCase.execute(command(
                alienMetadata(348L), MediaKind.MOVIE,
                CatalogItemVisibility.SHARED, List.of("ana", "ana", "  ", "luis"))))
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
                alienMetadata(null), MediaKind.MOVIE, CatalogItemVisibility.PRIVATE, List.of())))
        .expectError(IllegalArgumentException.class)
        .verify();

    verifyNoInteractions(this.movieRepository);
  }

  @Test
  void otherKindStaysRawEvenWithProviderId() {
    StepVerifier.create(
            this.useCase.execute(command(
                alienMetadata(348L), MediaKind.VIDEO, CatalogItemVisibility.PUBLIC, List.of())))
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
        org.mockito.Mockito.never()).updateAccess(any(CatalogItem.class));
    org.mockito.Mockito.verify(this.movieRepository,
        org.mockito.Mockito.never()).replaceShares(any(CatalogItem.class));
    org.mockito.Mockito.verify(this.movieRepository,
        org.mockito.Mockito.never()).updateVisibility(any(CatalogItem.class));
  }

  private void verifySavedThroughAtomicPort() {
    org.mockito.Mockito.verify(this.movieRepository).saveDraftWithAccess(any(CatalogItem.class));
  }
}
