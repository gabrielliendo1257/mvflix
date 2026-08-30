package com.gcorp.service.app.mvflix_movies.catalog.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.gcorp.service.app.mvflix_movies.shared.application.security.AuthenticatedUser;
import com.gcorp.service.app.mvflix_movies.shared.application.security.UserProvider;
import com.gcorp.service.app.mvflix_movies.catalog.domain.asset.ManagedMediaAsset;
import com.gcorp.service.app.mvflix_movies.catalog.domain.asset.MediaRepository;
import com.gcorp.service.app.mvflix_movies.catalog.domain.movie.EnrichmentStatus;
import com.gcorp.service.app.mvflix_movies.catalog.domain.item.CatalogItemKind;
import com.gcorp.service.app.mvflix_movies.catalog.domain.item.CatalogItem;
import com.gcorp.service.app.mvflix_movies.catalog.domain.item.CatalogItemConflictException;
import com.gcorp.service.app.mvflix_movies.catalog.domain.item.CatalogItemId;
import com.gcorp.service.app.mvflix_movies.catalog.domain.metadata.MovieMetadata;
import com.gcorp.service.app.mvflix_movies.catalog.domain.item.CatalogItemNotFoundException;
import com.gcorp.service.app.mvflix_movies.catalog.domain.item.CatalogItemRepository;
import com.gcorp.service.app.mvflix_movies.catalog.domain.item.CatalogItemStatus;
import com.gcorp.service.app.mvflix_movies.catalog.domain.access.Visibility;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

class CompleteCatalogItemUseCaseTest {

  private static final CatalogItemId MOVIE_ID = CatalogItemId.of(10L);
  private static final String OWNER = "owner-subject";
  private static final Long OBJECT_ID = 99L;
  private static final String OBJECT_KEY = "movies/10/video.mp4";

  private RecordingMovieRepository movieRepository;
  private RecordingMediaRepository mediaRepository;
  private StubUserProvider userProvider;
  private CompleteCatalogItemUseCase useCase;

  @BeforeEach
  void setUp() {
    this.movieRepository = new RecordingMovieRepository();
    this.mediaRepository = new RecordingMediaRepository();
    this.userProvider =
        new StubUserProvider(Mono.just(new AuthenticatedUser(OWNER, "owner@mvflix.test")));
    this.useCase =
        new CompleteCatalogItemUseCase(this.movieRepository, this.mediaRepository, this.userProvider);
  }

  @Test
  void completesOwnedDraftAndPersistsItsMedia() {
    this.movieRepository.findByIdReturns(draftMovie());
    this.movieRepository.completeIfDraftReturns(readyMovie(null));

    StepVerifier.create(this.useCase.execute(MOVIE_ID, OBJECT_ID, OBJECT_KEY))
        .assertNext(
            movie -> {
              assertThat(movie.getId()).isEqualTo(MOVIE_ID);
              assertThat(movie.getStatus()).isEqualTo(CatalogItemStatus.READY);
            })
        .verifyComplete();

    assertThat(this.movieRepository.completeIfDraftIds).containsExactly(MOVIE_ID);
    assertThat(this.mediaRepository.savedMedia)
        .singleElement()
        .satisfies(
            media -> {
              assertThat(media.getMovieId()).isEqualTo(MOVIE_ID);
              assertThat(media.getObjectId()).isEqualTo(OBJECT_ID);
              assertThat(media.getObjectKey()).isEqualTo(OBJECT_KEY);
            });
    assertThat(this.mediaRepository.findByCatalogItemIdIds).isEmpty();
  }

  @Test
  void rejectsMissingMovieWithoutTryingToCompleteOrSaveMedia() {
    this.movieRepository.findByIdReturnsEmpty();

    StepVerifier.create(this.useCase.execute(MOVIE_ID, OBJECT_ID, OBJECT_KEY))
        .expectError(CatalogItemNotFoundException.class)
        .verify();

    assertThat(this.movieRepository.completeIfDraftIds).isEmpty();
    assertThat(this.mediaRepository.savedMedia).isEmpty();
  }

  @Test
  void hidesMovieFromNonOwnerAsNotFound() {
    this.movieRepository.findByIdReturns(movieOwnedBy("another-owner", CatalogItemStatus.DRAFT, null));

    StepVerifier.create(this.useCase.execute(MOVIE_ID, OBJECT_ID, OBJECT_KEY))
        .expectError(CatalogItemNotFoundException.class)
        .verify();

    assertThat(this.movieRepository.completeIfDraftIds).isEmpty();
    assertThat(this.mediaRepository.savedMedia).isEmpty();
  }

  @Test
  void repeatedCompletionWithSameObjectKeyIsIdempotent() {
    this.movieRepository.findByIdReturns(readyMovie(null), readyMovie(null));
    this.movieRepository.completeIfDraftReturnsEmpty();
    this.mediaRepository.findByCatalogItemIdReturns(ManagedMediaAsset.create(MOVIE_ID, 123L, OBJECT_KEY));

    StepVerifier.create(this.useCase.execute(MOVIE_ID, OBJECT_ID, OBJECT_KEY))
        .assertNext(
            movie -> {
              assertThat(movie.getStatus()).isEqualTo(CatalogItemStatus.READY);
            })
        .verifyComplete();

    assertThat(this.movieRepository.findByIdIds).containsExactly(MOVIE_ID, MOVIE_ID);
    assertThat(this.movieRepository.completeIfDraftIds).containsExactly(MOVIE_ID);
    assertThat(this.mediaRepository.savedMedia).isEmpty();
    assertThat(this.mediaRepository.findByCatalogItemIdIds).containsExactly(MOVIE_ID);
  }

  @Test
  void rejectsRepeatedCompletionWithDifferentObjectKey() {
    this.movieRepository.findByIdReturns(readyMovie(null), readyMovie(null));
    this.movieRepository.completeIfDraftReturnsEmpty();
    this.mediaRepository.findByCatalogItemIdReturns(
        ManagedMediaAsset.create(MOVIE_ID, 123L, "movies/10/another.mp4"));

    StepVerifier.create(this.useCase.execute(MOVIE_ID, OBJECT_ID, OBJECT_KEY))
        .expectError(CatalogItemConflictException.class)
        .verify();

    assertThat(this.mediaRepository.savedMedia).isEmpty();
  }

  @Test
  void rejectsLostDraftRaceWhenMovieDidNotBecomeReady() {
    this.movieRepository.findByIdReturns(draftMovie(), draftMovie());
    this.movieRepository.completeIfDraftReturnsEmpty();

    StepVerifier.create(this.useCase.execute(MOVIE_ID, OBJECT_ID, OBJECT_KEY))
        .expectError(CatalogItemConflictException.class)
        .verify();

    assertThat(this.mediaRepository.findByCatalogItemIdIds).isEmpty();
    assertThat(this.mediaRepository.savedMedia).isEmpty();
  }

  @Test
  void reportsNotFoundWhenMovieDisappearsWhileResolvingConflict() {
    this.movieRepository.findByIdReturns(draftMovie());
    this.movieRepository.findByIdReturnsEmpty();
    this.movieRepository.completeIfDraftReturnsEmpty();

    StepVerifier.create(this.useCase.execute(MOVIE_ID, OBJECT_ID, OBJECT_KEY))
        .expectError(CatalogItemNotFoundException.class)
        .verify();

    assertThat(this.mediaRepository.savedMedia).isEmpty();
  }

  @Test
  void rejectsReadyMovieWithoutUploadMediaAsConflict() {
    this.movieRepository.findByIdReturns(readyMovie(null), readyMovie(null));
    this.movieRepository.completeIfDraftReturnsEmpty();
    this.mediaRepository.findByCatalogItemIdReturnsEmpty();

    StepVerifier.create(this.useCase.execute(MOVIE_ID, OBJECT_ID, OBJECT_KEY))
        .expectError(CatalogItemConflictException.class)
        .verify();

    assertThat(this.mediaRepository.savedMedia).isEmpty();
  }

  @Test
  void propagatesAuthenticationFailureWithoutCallingRepositories() {
    IllegalStateException failure = new IllegalStateException("authentication unavailable");
    this.userProvider.result = Mono.error(failure);

    StepVerifier.create(this.useCase.execute(MOVIE_ID, OBJECT_ID, OBJECT_KEY))
        .expectErrorSatisfies(error -> assertThat(error).isSameAs(failure))
        .verify();

    assertThat(this.movieRepository.findByIdIds).isEmpty();
    assertThat(this.movieRepository.completeIfDraftIds).isEmpty();
    assertThat(this.mediaRepository.savedMedia).isEmpty();
  }

  @Test
  void propagatesCompleteIfDraftFailureWithoutSavingMedia() {
    IllegalStateException failure = new IllegalStateException("database unavailable");
    this.movieRepository.findByIdReturns(draftMovie());
    this.movieRepository.completeIfDraftResult = Mono.error(failure);

    StepVerifier.create(this.useCase.execute(MOVIE_ID, OBJECT_ID, OBJECT_KEY))
        .expectErrorSatisfies(error -> assertThat(error).isSameAs(failure))
        .verify();

    assertThat(this.mediaRepository.savedMedia).isEmpty();
  }

  @Test
  void propagatesMediaSaveFailure() {
    IllegalStateException failure = new IllegalStateException("media insert failed");
    this.movieRepository.findByIdReturns(draftMovie());
    this.movieRepository.completeIfDraftReturns(readyMovie(null));
    this.mediaRepository.saveResult = ignored -> Mono.error(failure);

    StepVerifier.create(this.useCase.execute(MOVIE_ID, OBJECT_ID, OBJECT_KEY))
        .expectErrorSatisfies(error -> assertThat(error).isSameAs(failure))
        .verify();

    assertThat(this.mediaRepository.savedMedia)
        .singleElement()
        .satisfies(media -> assertThat(media.getObjectKey()).isEqualTo(OBJECT_KEY));
  }

  @Test
  void propagatesMediaLookupFailureWhileResolvingRetry() {
    IllegalStateException failure = new IllegalStateException("media query failed");
    this.movieRepository.findByIdReturns(readyMovie(null), readyMovie(null));
    this.movieRepository.completeIfDraftReturnsEmpty();
    this.mediaRepository.findByCatalogItemIdResult = Mono.error(failure);

    StepVerifier.create(this.useCase.execute(MOVIE_ID, OBJECT_ID, OBJECT_KEY))
        .expectErrorSatisfies(error -> assertThat(error).isSameAs(failure))
        .verify();

    assertThat(this.mediaRepository.savedMedia).isEmpty();
  }

  private static CatalogItem draftMovie() {
    return movieOwnedBy(OWNER, CatalogItemStatus.DRAFT, null);
  }

  private static CatalogItem readyMovie(Long objectId) {
    return movieOwnedBy(OWNER, CatalogItemStatus.READY, objectId);
  }

  private static CatalogItem movieOwnedBy(String owner, CatalogItemStatus status, Long objectId) {
    return new CatalogItem(
        MOVIE_ID,
        owner,
        "Dune",
        status,
        EnrichmentStatus.RAW,
        objectId,
        MovieMetadata.onlyTitle("Dune"),
        Visibility.PRIVATE,
        Set.of(),
        CatalogItemKind.MOVIE);
  }

  private static final class StubUserProvider implements UserProvider {

    private Mono<AuthenticatedUser> result;

    private StubUserProvider(Mono<AuthenticatedUser> result) {
      this.result = result;
    }

    @Override
    public Mono<AuthenticatedUser> getAuthenticatedUser() {
      return this.result;
    }
  }

  private static final class RecordingMediaRepository implements MediaRepository {

    private final List<ManagedMediaAsset> savedMedia = new ArrayList<>();
    private final List<CatalogItemId> findByCatalogItemIdIds = new ArrayList<>();
    private Function<ManagedMediaAsset, Mono<ManagedMediaAsset>> saveResult = Mono::just;
    private Mono<ManagedMediaAsset> findByCatalogItemIdResult = unexpectedMono("findByCatalogItemId");

    @Override
    public Mono<ManagedMediaAsset> save(ManagedMediaAsset media) {
      this.savedMedia.add(media);
      return this.saveResult.apply(media);
    }

    @Override
    public Mono<ManagedMediaAsset> findByCatalogItemId(CatalogItemId catalogItemId) {
      this.findByCatalogItemIdIds.add(catalogItemId);
      return this.findByCatalogItemIdResult;
    }

    private void findByCatalogItemIdReturns(ManagedMediaAsset media) {
      this.findByCatalogItemIdResult = Mono.just(media);
    }

    private void findByCatalogItemIdReturnsEmpty() {
      this.findByCatalogItemIdResult = Mono.empty();
    }
  }

  private static final class RecordingMovieRepository implements CatalogItemRepository {

    private final Deque<Mono<CatalogItem>> findByIdResults = new ArrayDeque<>();
    private final List<CatalogItemId> findByIdIds = new ArrayList<>();
    private final List<CatalogItemId> completeIfDraftIds = new ArrayList<>();
    private Mono<CatalogItem> completeIfDraftResult = unexpectedMono("completeIfDraft");

    @Override
    public Mono<CatalogItem> saveDraftWithAccess(CatalogItem movie) {
      return unexpectedMono("saveDraftWithAccess");
    }

    @Override
    public Mono<CatalogItem> save(CatalogItem movie) {
      return unexpectedMono("save");
    }

    @Override
    public Mono<CatalogItem> findById(CatalogItemId id) {
      this.findByIdIds.add(id);
      if (this.findByIdResults.isEmpty()) {
        return unexpectedMono("findById");
      }
      return this.findByIdResults.removeFirst();
    }

    @Override
    public Flux<CatalogItem> findVisibleCatalogItems(String username, int limit) {
      return unexpectedFlux("findVisibleCatalogItems");
    }

    @Override
    public Flux<CatalogItem> findByOwner(String ownerUsername, int limit) {
      return unexpectedFlux("findByOwner");
    }

    @Override
    public Flux<CatalogItem> findByOwnerAndIds(String ownerUsername, List<CatalogItemId> ids) {
      return unexpectedFlux("findByOwnerAndIds");
    }

    @Override
    public Mono<CatalogItem> completeIfDraft(CatalogItemId id) {
      this.completeIfDraftIds.add(id);
      return this.completeIfDraftResult;
    }

    @Override
    public Mono<Boolean> deleteById(CatalogItemId id) {
      return unexpectedMono("deleteById");
    }

    @Override
    public Mono<CatalogItem> markDeleting(CatalogItemId id) {
      return unexpectedMono("markDeleting");
    }

    @Override
    public Mono<Boolean> deleteIfDeleting(CatalogItemId id) {
      return unexpectedMono("deleteIfDeleting");
    }

    @Override
    public Mono<Boolean> deleteIfDeletingAndStorageId(CatalogItemId id, long storageId) {
      return unexpectedMono("deleteIfDeletingAndStorageId");
    }

    @Override
    public Flux<CatalogItem> findDeleting(int limit) {
      return unexpectedFlux("findDeleting");
    }

    @Override
    public Flux<CatalogItem> findDeletingForRecovery(int limit, Duration retryCooldown) {
      return unexpectedFlux("findDeletingForRecovery");
    }

    @Override
    public Mono<Void> markRecoveryAttempt(CatalogItemId id) {
      return unexpectedMono("markRecoveryAttempt");
    }

    @Override
    public Mono<CatalogItem> updateEnrichment(CatalogItem movie) {
      return unexpectedMono("updateEnrichment");
    }

    @Override
    public Mono<CatalogItem> updateDetails(CatalogItem movie) {
      return unexpectedMono("updateDetails");
    }

    @Override
    public Mono<CatalogItem> updateVisibility(CatalogItem movie) {
      return unexpectedMono("updateVisibility");
    }

    @Override
    public Mono<CatalogItem> replaceShares(CatalogItem movie) {
      return unexpectedMono("replaceShares");
    }

    @Override
    public Mono<CatalogItem> updateAccess(CatalogItem movie) {
      return unexpectedMono("updateAccess");
    }

    @Override
    public Flux<CatalogItem> findByEnrichmentStatus(EnrichmentStatus enrichmentStatus, int limit) {
      return unexpectedFlux("findByEnrichmentStatus");
    }

    @Override
    public Mono<Long> deleteDraftsCreatedBefore(Instant cutoff) {
      return unexpectedMono("deleteDraftsCreatedBefore");
    }

    private void findByIdReturns(CatalogItem... movies) {
      for (CatalogItem movie : movies) {
        this.findByIdResults.addLast(Mono.just(movie));
      }
    }

    private void findByIdReturnsEmpty() {
      this.findByIdResults.addLast(Mono.empty());
    }

    private void completeIfDraftReturns(CatalogItem movie) {
      this.completeIfDraftResult = Mono.just(movie);
    }

    private void completeIfDraftReturnsEmpty() {
      this.completeIfDraftResult = Mono.empty();
    }
  }

  private static <T> Mono<T> unexpectedMono(String operation) {
    return Mono.error(new AssertionError("Unexpected repository call: " + operation));
  }

  private static <T> Flux<T> unexpectedFlux(String operation) {
    return Flux.error(new AssertionError("Unexpected repository call: " + operation));
  }
}
