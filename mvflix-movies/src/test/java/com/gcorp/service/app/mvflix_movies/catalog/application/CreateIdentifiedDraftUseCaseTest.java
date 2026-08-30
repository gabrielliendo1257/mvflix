package com.gcorp.service.app.mvflix_movies.catalog.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.gcorp.service.app.mvflix_movies.catalog.domain.item.CatalogItemKind;
import com.gcorp.service.app.mvflix_movies.catalog.domain.item.CatalogItem;
import com.gcorp.service.app.mvflix_movies.catalog.domain.item.CatalogItemId;
import com.gcorp.service.app.mvflix_movies.catalog.domain.item.CatalogItemStatus;
import com.gcorp.service.app.mvflix_movies.catalog.domain.metadata.MovieMetadata;
import com.gcorp.service.app.mvflix_movies.catalog.domain.movie.EnrichmentStatus;
import com.gcorp.service.app.mvflix_movies.catalog.domain.item.CatalogItemRepository;
import com.gcorp.service.app.mvflix_movies.catalog.application.port.IdentifiedDraftIdempotencyStore;
import com.gcorp.service.app.mvflix_movies.catalog.domain.access.Visibility;
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
import java.util.concurrent.atomic.AtomicReference;

@ExtendWith(MockitoExtension.class)
class CreateIdentifiedDraftUseCaseTest {

  @Mock private CatalogItemRepository movieRepository;
  @Mock private UserProvider userProvider;
  @Mock private IdentifiedDraftIdempotencyStore idempotencyStore;

  private CreateIdentifiedDraftUseCase useCase;

  @BeforeEach
  void setUp() {
    this.useCase = new CreateIdentifiedDraftUseCase(this.movieRepository, this.userProvider,
        null, this.idempotencyStore);
    org.mockito.Mockito.lenient()
        .when(this.userProvider.getAuthenticatedUser())
        .thenReturn(Mono.just(new AuthenticatedUser("pepe", "pepe@test")));
    // Simula el INSERT ... RETURNING: la fila guardada sale CON id asignado.
    org.mockito.Mockito.lenient()
        .when(this.movieRepository.saveDraftWithAccess(any(CatalogItem.class)))
        .thenAnswer(invocation -> {
          CatalogItem in = invocation.getArgument(0);
          return Mono.just(new CatalogItem(
              com.gcorp.service.app.mvflix_movies.catalog.domain.item.CatalogItemId.of(1L),
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
      MovieMetadata metadata, CatalogItemKind kind,
      Visibility visibility, List<String> shared) {
    return new CreateIdentifiedDraftCommand(metadata, kind, visibility, shared);
  }

  @Test
  void createsEnrichedDraftWithPrivateDefaultAccess() {
    StepVerifier.create(
            this.useCase.execute(command(
                alienMetadata(348L), CatalogItemKind.MOVIE, null, List.of())))
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
                alienMetadata(348L), CatalogItemKind.MOVIE,
                Visibility.SHARED, List.of())))
        .expectError(IllegalArgumentException.class)
        .verify();

    verifyNoInteractions(this.movieRepository);
  }

  @Test
  void sharedCleansBlankAndDuplicateUsernames() {
    StepVerifier.create(
            this.useCase.execute(command(
                alienMetadata(348L), CatalogItemKind.MOVIE,
                Visibility.SHARED, List.of("ana", "ana", "  ", "luis"))))
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
                alienMetadata(null), CatalogItemKind.MOVIE, Visibility.PRIVATE, List.of())))
        .expectError(IllegalArgumentException.class)
        .verify();

    verifyNoInteractions(this.movieRepository);
  }

  @Test
  void otherKindStaysRawEvenWithProviderId() {
    StepVerifier.create(
            this.useCase.execute(command(
                alienMetadata(348L), CatalogItemKind.VIDEO, Visibility.PUBLIC, List.of())))
        .assertNext(movie -> {
          assertThat(movie.getEnrichmentStatus().name()).isEqualTo("RAW");
          assertThat(movie.getVisibility().name()).isEqualTo("PUBLIC");
        })
        .verifyComplete();
  }

  @Test
  void sameIdempotencyKeyReturnsTheOriginalDraft() {
    var command = command(alienMetadata(348L), CatalogItemKind.MOVIE,
        Visibility.PRIVATE, List.of());
    command = new CreateIdentifiedDraftCommand(command.metadata(), command.kind(),
        command.visibility(), command.sharedWith(), "ingestion:create-catalog-draft");
    var hash = new AtomicReference<String>();
    when(this.idempotencyStore.claim(anyString(), anyString(), anyString(), anyString()))
        .thenAnswer(invocation -> {
          if (hash.get() == null) {
            hash.set(invocation.getArgument(3));
            return Mono.just(new IdentifiedDraftIdempotencyStore.Claim(
                "pepe", "create-catalog-draft", "ingestion:create-catalog-draft",
                hash.get(), null));
          }
          return Mono.just(new IdentifiedDraftIdempotencyStore.Claim(
              "pepe", "create-catalog-draft", "ingestion:create-catalog-draft",
              hash.get(), com.gcorp.service.app.mvflix_movies.catalog.domain.item.CatalogItemId.of(1L)));
        });
    var original = new CatalogItem(CatalogItemId.of(1L), "pepe", "Alien",
        CatalogItemStatus.DRAFT, EnrichmentStatus.ENRICHED, null, alienMetadata(348L),
        Visibility.PRIVATE, java.util.Set.of(), CatalogItemKind.MOVIE);
    when(this.movieRepository.findById(any())).thenReturn(Mono.just(original));
    when(this.idempotencyStore.bind(anyString(), anyString(), anyString(), any()))
        .thenReturn(Mono.empty());

    this.useCase.execute(command).block();
    this.useCase.execute(command).block();

    org.mockito.Mockito.verify(this.movieRepository,
        org.mockito.Mockito.times(1)).saveDraftWithAccess(any(CatalogItem.class));
  }

  @Test
  void reusedIdempotencyKeyWithDifferentPayloadIsRejected() {
    var command = new CreateIdentifiedDraftCommand(alienMetadata(348L), CatalogItemKind.MOVIE,
        Visibility.PRIVATE, List.of(), "same-key");
    when(this.idempotencyStore.claim(anyString(), anyString(), anyString(), anyString()))
        .thenReturn(Mono.just(new IdentifiedDraftIdempotencyStore.Claim(
            "pepe", "create-catalog-draft", "same-key", "different-hash", null)));

    StepVerifier.create(this.useCase.execute(command))
        .expectError(IdempotencyKeyReusedException.class)
        .verify();

    org.mockito.Mockito.verify(this.movieRepository,
        org.mockito.Mockito.never()).saveDraftWithAccess(any(CatalogItem.class));
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
