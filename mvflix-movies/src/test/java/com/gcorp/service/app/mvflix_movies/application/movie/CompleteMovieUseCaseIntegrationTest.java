package com.gcorp.service.app.mvflix_movies.application.movie;

import static org.assertj.core.api.Assertions.assertThat;

import com.gcorp.service.app.mvflix_movies.support.PostgresIntegrationTest;
import com.gcorp.service.app.mvflix_movies.catalog.domain.movie.MovieId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.test.context.ActiveProfiles;
import reactor.test.StepVerifier;

@ActiveProfiles("sandbox")
@SpringBootTest
class CompleteMovieUseCaseIntegrationTest extends PostgresIntegrationTest {

  private static final String DUPLICATE_OBJECT_KEY = "movies/shared/video.mp4";

  @Autowired private CompleteMovieUseCase useCase;
  @Autowired private DatabaseClient databaseClient;

  @BeforeEach
  void cleanDatabase() {
    this.databaseClient.sql("DELETE FROM movies").fetch().rowsUpdated().block();
  }

  @Test
  void rollsBackReadyTransitionWhenMediaInsertFails() {
    Long movieWithMedia = this.insertMovie("READY");
    this.insertMedia(movieWithMedia, 700L, DUPLICATE_OBJECT_KEY);
    Long draftMovie = this.insertMovie("DRAFT");

    StepVerifier.create(
            this.useCase.execute(MovieId.of(draftMovie), 701L, DUPLICATE_OBJECT_KEY))
        .expectErrorSatisfies(
            error -> assertThat(error).isInstanceOf(DataIntegrityViolationException.class))
        .verify();

    StepVerifier.create(this.movieStatus(draftMovie)).expectNext("DRAFT").verifyComplete();
    StepVerifier.create(this.mediaCount(draftMovie)).expectNext(0L).verifyComplete();
  }

  private Long insertMovie(String status) {
    return this.databaseClient
        .sql(
            """
            INSERT INTO movies (
                owner_username, title, status, enrichment_status, metadata, visibility, kind)
            VALUES ('pepe', 'Dune', :status, 'RAW', '{"title":"Dune"}'::jsonb,
                    'PRIVATE', 'MOVIE')
            RETURNING id
            """)
        .bind("status", status)
        .map((row, metadata) -> row.get("id", Long.class))
        .one()
        .block();
  }

  private void insertMedia(Long movieId, Long objectId, String objectKey) {
    this.databaseClient
        .sql(
            """
            INSERT INTO media (movie_id, object_id, object_key)
            VALUES (:movie_id, :object_id, :object_key)
            """)
        .bind("movie_id", movieId)
        .bind("object_id", objectId)
        .bind("object_key", objectKey)
        .fetch()
        .rowsUpdated()
        .block();
  }

  private reactor.core.publisher.Mono<String> movieStatus(Long movieId) {
    return this.databaseClient
        .sql("SELECT status FROM movies WHERE id = :id")
        .bind("id", movieId)
        .map((row, metadata) -> row.get("status", String.class))
        .one();
  }

  private reactor.core.publisher.Mono<Long> mediaCount(Long movieId) {
    return this.databaseClient
        .sql("SELECT COUNT(*) AS count FROM media WHERE movie_id = :movie_id")
        .bind("movie_id", movieId)
        .map((row, metadata) -> row.get("count", Long.class))
        .one();
  }
}
