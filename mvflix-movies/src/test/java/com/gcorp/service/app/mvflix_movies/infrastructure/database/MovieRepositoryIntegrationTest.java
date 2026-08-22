package com.gcorp.service.app.mvflix_movies.infrastructure.database;

import static org.assertj.core.api.Assertions.assertThat;

import com.gcorp.service.app.mvflix_movies.domain.movie.EnrichmentStatus;
import com.gcorp.service.app.mvflix_movies.domain.movie.MediaKind;
import com.gcorp.service.app.mvflix_movies.domain.movie.Movie;
import com.gcorp.service.app.mvflix_movies.domain.movie.MovieMetadata;
import com.gcorp.service.app.mvflix_movies.domain.movie.MovieRepository;
import com.gcorp.service.app.mvflix_movies.support.PostgresIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles("sandbox")
@SpringBootTest
class MovieRepositoryIntegrationTest extends PostgresIntegrationTest {

  @Autowired private MovieRepository movieRepository;
  @Autowired private DatabaseClient databaseClient;

  @BeforeEach
  void cleanDatabase() {
    this.databaseClient.sql("DELETE FROM movies").fetch().rowsUpdated().block();
  }

  @Test
  void updateMetadataSynchronizesCatalogTitle() {
    Movie movie = this.saveDraft("Old title");

    Movie updated =
        this.movieRepository
            .updateMetadata(movie.getId(), MovieMetadata.onlyTitle("New title"))
            .block();

    assertThat(updated).isNotNull();
    assertThat(updated.getTitle()).isEqualTo("New title");
    this.assertPersistedTitles(movie.getId().value(), "New title");
  }

  @Test
  void updateEnrichmentSynchronizesCatalogTitle() {
    Movie movie = this.saveDraft("Local title");

    Movie updated =
        this.movieRepository
            .updateEnrichment(
                movie.getId(),
                MovieMetadata.onlyTitle("Provider title"),
                EnrichmentStatus.ENRICHED)
            .block();

    assertThat(updated).isNotNull();
    assertThat(updated.getTitle()).isEqualTo("Provider title");
    assertThat(updated.getEnrichmentStatus()).isEqualTo(EnrichmentStatus.ENRICHED);
    this.assertPersistedTitles(movie.getId().value(), "Provider title");
  }

  private Movie saveDraft(String title) {
    return this.movieRepository
        .save(Movie.createDraft("pepe", MovieMetadata.onlyTitle(title), MediaKind.MOVIE))
        .block();
  }

  private void assertPersistedTitles(Long movieId, String expectedTitle) {
    TitleProjection projection =
        this.databaseClient
            .sql(
                """
                SELECT title, metadata ->> 'title' AS metadata_title
                FROM movies
                WHERE id = :id
                """)
            .bind("id", movieId)
            .map(
                (row, metadata) ->
                    new TitleProjection(
                        row.get("title", String.class),
                        row.get("metadata_title", String.class)))
            .one()
            .block();

    assertThat(projection).isEqualTo(new TitleProjection(expectedTitle, expectedTitle));
  }

  private record TitleProjection(String catalogTitle, String metadataTitle) {}
}
