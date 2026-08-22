package com.gcorp.service.app.mvflix_movies.catalog.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.gcorp.service.app.mvflix_movies.catalog.domain.movie.MediaKind;
import com.gcorp.service.app.mvflix_movies.catalog.domain.movie.Movie;
import com.gcorp.service.app.mvflix_movies.catalog.domain.movie.MovieMetadata;
import com.gcorp.service.app.mvflix_movies.catalog.domain.movie.MovieRepository;
import com.gcorp.service.app.mvflix_movies.support.PostgresIntegrationTest;
import org.junit.jupiter.api.AfterEach;
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
class UpdateMovieUseCaseIntegrationTest extends PostgresIntegrationTest {

  private static final String TEST_CONSTRAINT = "test_movie_kind_must_remain_movie";

  @Autowired private UpdateMovieUseCase useCase;
  @Autowired private MovieRepository movieRepository;
  @Autowired private DatabaseClient databaseClient;

  @BeforeEach
  void setUp() {
    this.dropTestConstraint();
    this.databaseClient.sql("DELETE FROM movies").fetch().rowsUpdated().block();
  }

  @AfterEach
  void tearDown() {
    this.dropTestConstraint();
  }

  @Test
  void doesNotPersistPartialDetailsWhenReclassificationFails() {
    Movie movie =
        this.movieRepository
            .save(
                Movie.createDraft(
                    "pepe", MovieMetadata.onlyTitle("Original title"), MediaKind.MOVIE))
            .block();
    this.databaseClient
        .sql(
            "ALTER TABLE movies ADD CONSTRAINT "
                + TEST_CONSTRAINT
                + " CHECK (kind = 'MOVIE')")
        .fetch()
        .rowsUpdated()
        .block();

    StepVerifier.create(this.useCase.execute(movie.getId(), switchToOther("Edited title")))
        .expectError(DataIntegrityViolationException.class)
        .verify();

    Movie persisted = this.movieRepository.findById(movie.getId()).block();
    assertThat(persisted).isNotNull();
    assertThat(persisted.getKind()).isEqualTo(MediaKind.MOVIE);
    assertThat(persisted.getTitle()).isEqualTo("Original title");
    assertThat(persisted.getMetadata().title()).isEqualTo("Original title");
  }

  private static UpdateMovieCommand switchToOther(String title) {
    return new UpdateMovieCommand(
        title,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        MediaKind.OTHER);
  }

  private void dropTestConstraint() {
    this.databaseClient
        .sql("ALTER TABLE movies DROP CONSTRAINT IF EXISTS " + TEST_CONSTRAINT)
        .fetch()
        .rowsUpdated()
        .block();
  }
}
