package com.gcorp.service.app.mvflix_movies.catalog.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.gcorp.service.app.mvflix_movies.app.security.AuthenticatedUser;
import com.gcorp.service.app.mvflix_movies.app.security.UserProvider;
import com.gcorp.service.app.mvflix_movies.catalog.domain.media.Media;
import com.gcorp.service.app.mvflix_movies.catalog.domain.media.MediaRepository;
import com.gcorp.service.app.mvflix_movies.catalog.domain.movie.EnrichmentStatus;
import com.gcorp.service.app.mvflix_movies.catalog.domain.movie.MediaKind;
import com.gcorp.service.app.mvflix_movies.catalog.domain.movie.Movie;
import com.gcorp.service.app.mvflix_movies.catalog.domain.movie.MovieConflictException;
import com.gcorp.service.app.mvflix_movies.catalog.domain.movie.MovieId;
import com.gcorp.service.app.mvflix_movies.catalog.domain.movie.MovieMetadata;
import com.gcorp.service.app.mvflix_movies.catalog.domain.movie.MovieNotFoundException;
import com.gcorp.service.app.mvflix_movies.catalog.domain.movie.MovieRepository;
import com.gcorp.service.app.mvflix_movies.catalog.domain.movie.MovieStatus;
import com.gcorp.service.app.mvflix_movies.catalog.domain.movie.MovieVisibility;
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

class CompleteMovieUseCaseTest {

  private static final MovieId MOVIE_ID = MovieId.of(10L);
  private static final String OWNER = "owner-subject";
  private static final Long OBJECT_ID = 99L;
  private static final String OBJECT_KEY = "movies/10/video.mp4";

  private RecordingMovieRepository movieRepository;
  private RecordingMediaRepository mediaRepository;
  private StubUserProvider userProvider;
  private CompleteMovieUseCase useCase;

  @BeforeEach
  void setUp() {
    this.movieRepository = new RecordingMovieRepository();
    this.mediaRepository = new RecordingMediaRepository();
    this.userProvider =
        new StubUserProvider(Mono.just(new AuthenticatedUser(OWNER, "owner@mvflix.test")));
    this.useCase =
        new CompleteMovieUseCase(this.movieRepository, this.mediaRepository, this.userProvider);
  }

  @Test
  void completesOwnedDraftAndPersistsItsMedia() {
    this.movieRepository.findByIdReturns(draftMovie());
    this.movieRepository.completeIfDraftReturns(readyMovie(null));

    StepVerifier.create(this.useCase.execute(MOVIE_ID, OBJECT_ID, OBJECT_KEY))
        .assertNext(
            movie -> {
              assertThat(movie.getId()).isEqualTo(MOVIE_ID);
              assertThat(movie.getStatus()).isEqualTo(MovieStatus.READY);
              assertThat(movie.getObjectId()).isEqualTo(OBJECT_ID);
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
    assertThat(this.mediaRepository.findByMovieIdIds).isEmpty();
  }

  @Test
  void rejectsMissingMovieWithoutTryingToCompleteOrSaveMedia() {
    this.movieRepository.findByIdReturnsEmpty();

    StepVerifier.create(this.useCase.execute(MOVIE_ID, OBJECT_ID, OBJECT_KEY))
        .expectError(MovieNotFoundException.class)
        .verify();

    assertThat(this.movieRepository.completeIfDraftIds).isEmpty();
    assertThat(this.mediaRepository.savedMedia).isEmpty();
  }

  @Test
  void hidesMovieFromNonOwnerAsNotFound() {
    this.movieRepository.findByIdReturns(movieOwnedBy("another-owner", MovieStatus.DRAFT, null));

    StepVerifier.create(this.useCase.execute(MOVIE_ID, OBJECT_ID, OBJECT_KEY))
        .expectError(MovieNotFoundException.class)
        .verify();

    assertThat(this.movieRepository.completeIfDraftIds).isEmpty();
    assertThat(this.mediaRepository.savedMedia).isEmpty();
  }

  @Test
  void repeatedCompletionWithSameObjectKeyIsIdempotent() {
    this.movieRepository.findByIdReturns(readyMovie(null), readyMovie(null));
    this.movieRepository.completeIfDraftReturnsEmpty();
    this.mediaRepository.findByMovieIdReturns(Media.create(MOVIE_ID, 123L, OBJECT_KEY));

    StepVerifier.create(this.useCase.execute(MOVIE_ID, OBJECT_ID, OBJECT_KEY))
        .assertNext(
            movie -> {
              assertThat(movie.getStatus()).isEqualTo(MovieStatus.READY);
              assertThat(movie.getObjectId()).isEqualTo(123L);
            })
        .verifyComplete();

    assertThat(this.movieRepository.findByIdIds).containsExactly(MOVIE_ID, MOVIE_ID);
    assertThat(this.movieRepository.completeIfDraftIds).containsExactly(MOVIE_ID);
    assertThat(this.mediaRepository.savedMedia).isEmpty();
    assertThat(this.mediaRepository.findByMovieIdIds).containsExactly(MOVIE_ID);
  }

  @Test
  void rejectsRepeatedCompletionWithDifferentObjectKey() {
    this.movieRepository.findByIdReturns(readyMovie(null), readyMovie(null));
    this.movieRepository.completeIfDraftReturnsEmpty();
    this.mediaRepository.findByMovieIdReturns(
        Media.create(MOVIE_ID, 123L, "movies/10/another.mp4"));

    StepVerifier.create(this.useCase.execute(MOVIE_ID, OBJECT_ID, OBJECT_KEY))
        .expectError(MovieConflictException.class)
        .verify();

    assertThat(this.mediaRepository.savedMedia).isEmpty();
  }

  @Test
  void rejectsLostDraftRaceWhenMovieDidNotBecomeReady() {
    this.movieRepository.findByIdReturns(draftMovie(), draftMovie());
    this.movieRepository.completeIfDraftReturnsEmpty();

    StepVerifier.create(this.useCase.execute(MOVIE_ID, OBJECT_ID, OBJECT_KEY))
        .expectError(MovieConflictException.class)
        .verify();

    assertThat(this.mediaRepository.findByMovieIdIds).isEmpty();
    assertThat(this.mediaRepository.savedMedia).isEmpty();
  }

  @Test
  void reportsNotFoundWhenMovieDisappearsWhileResolvingConflict() {
    this.movieRepository.findByIdReturns(draftMovie());
    this.movieRepository.findByIdReturnsEmpty();
    this.movieRepository.completeIfDraftReturnsEmpty();

    StepVerifier.create(this.useCase.execute(MOVIE_ID, OBJECT_ID, OBJECT_KEY))
        .expectError(MovieNotFoundException.class)
        .verify();

    assertThat(this.mediaRepository.savedMedia).isEmpty();
  }

  @Test
  void rejectsReadyMovieWithoutUploadMediaAsConflict() {
    this.movieRepository.findByIdReturns(readyMovie(null), readyMovie(null));
    this.movieRepository.completeIfDraftReturnsEmpty();
    this.mediaRepository.findByMovieIdReturnsEmpty();

    StepVerifier.create(this.useCase.execute(MOVIE_ID, OBJECT_ID, OBJECT_KEY))
        .expectError(MovieConflictException.class)
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
    this.mediaRepository.findByMovieIdResult = Mono.error(failure);

    StepVerifier.create(this.useCase.execute(MOVIE_ID, OBJECT_ID, OBJECT_KEY))
        .expectErrorSatisfies(error -> assertThat(error).isSameAs(failure))
        .verify();

    assertThat(this.mediaRepository.savedMedia).isEmpty();
  }

  private static Movie draftMovie() {
    return movieOwnedBy(OWNER, MovieStatus.DRAFT, null);
  }

  private static Movie readyMovie(Long objectId) {
    return movieOwnedBy(OWNER, MovieStatus.READY, objectId);
  }

  private static Movie movieOwnedBy(String owner, MovieStatus status, Long objectId) {
    return new Movie(
        MOVIE_ID,
        owner,
        "Dune",
        status,
        EnrichmentStatus.RAW,
        objectId,
        MovieMetadata.onlyTitle("Dune"),
        MovieVisibility.PRIVATE,
        Set.of(),
        MediaKind.MOVIE);
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

    private final List<Media> savedMedia = new ArrayList<>();
    private final List<MovieId> findByMovieIdIds = new ArrayList<>();
    private Function<Media, Mono<Media>> saveResult = Mono::just;
    private Mono<Media> findByMovieIdResult = unexpectedMono("findByMovieId");

    @Override
    public Mono<Media> save(Media media) {
      this.savedMedia.add(media);
      return this.saveResult.apply(media);
    }

    @Override
    public Mono<Media> findByMovieId(MovieId movieId) {
      this.findByMovieIdIds.add(movieId);
      return this.findByMovieIdResult;
    }

    private void findByMovieIdReturns(Media media) {
      this.findByMovieIdResult = Mono.just(media);
    }

    private void findByMovieIdReturnsEmpty() {
      this.findByMovieIdResult = Mono.empty();
    }
  }

  private static final class RecordingMovieRepository implements MovieRepository {

    private final Deque<Mono<Movie>> findByIdResults = new ArrayDeque<>();
    private final List<MovieId> findByIdIds = new ArrayList<>();
    private final List<MovieId> completeIfDraftIds = new ArrayList<>();
    private Mono<Movie> completeIfDraftResult = unexpectedMono("completeIfDraft");

    @Override
    public Mono<Movie> save(Movie movie) {
      return unexpectedMono("save");
    }

    @Override
    public Mono<Movie> findById(MovieId id) {
      this.findByIdIds.add(id);
      if (this.findByIdResults.isEmpty()) {
        return unexpectedMono("findById");
      }
      return this.findByIdResults.removeFirst();
    }

    @Override
    public Flux<Movie> findVisibleMovies(String username, int limit) {
      return unexpectedFlux("findVisibleMovies");
    }

    @Override
    public Flux<Movie> findByOwner(String ownerUsername, int limit) {
      return unexpectedFlux("findByOwner");
    }

    @Override
    public Flux<Movie> findByOwnerAndIds(String ownerUsername, List<MovieId> ids) {
      return unexpectedFlux("findByOwnerAndIds");
    }

    @Override
    public Mono<Movie> completeIfDraft(MovieId id) {
      this.completeIfDraftIds.add(id);
      return this.completeIfDraftResult;
    }

    @Override
    public Mono<Boolean> deleteById(MovieId id) {
      return unexpectedMono("deleteById");
    }

    @Override
    public Mono<Movie> updateEnrichment(Movie movie) {
      return unexpectedMono("updateEnrichment");
    }

    @Override
    public Mono<Movie> updateDetails(Movie movie) {
      return unexpectedMono("updateDetails");
    }

    @Override
    public Mono<Movie> updateVisibility(Movie movie) {
      return unexpectedMono("updateVisibility");
    }

    @Override
    public Mono<Movie> replaceShares(Movie movie) {
      return unexpectedMono("replaceShares");
    }

    @Override
    public Mono<Movie> updateAccess(Movie movie) {
      return unexpectedMono("updateAccess");
    }

    @Override
    public Flux<Movie> findByEnrichmentStatus(EnrichmentStatus enrichmentStatus, int limit) {
      return unexpectedFlux("findByEnrichmentStatus");
    }

    @Override
    public Mono<Long> deleteDraftsCreatedBefore(Instant cutoff) {
      return unexpectedMono("deleteDraftsCreatedBefore");
    }

    private void findByIdReturns(Movie... movies) {
      for (Movie movie : movies) {
        this.findByIdResults.addLast(Mono.just(movie));
      }
    }

    private void findByIdReturnsEmpty() {
      this.findByIdResults.addLast(Mono.empty());
    }

    private void completeIfDraftReturns(Movie movie) {
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
