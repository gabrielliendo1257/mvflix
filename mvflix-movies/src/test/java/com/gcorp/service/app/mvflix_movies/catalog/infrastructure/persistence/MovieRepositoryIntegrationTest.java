package com.gcorp.service.app.mvflix_movies.catalog.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.gcorp.service.app.mvflix_movies.catalog.domain.movie.EnrichmentStatus;
import com.gcorp.service.app.mvflix_movies.catalog.domain.movie.MediaKind;
import com.gcorp.service.app.mvflix_movies.catalog.domain.movie.Movie;
import com.gcorp.service.app.mvflix_movies.catalog.domain.movie.MovieMetadata;
import com.gcorp.service.app.mvflix_movies.catalog.domain.movie.MovieRepository;
import com.gcorp.service.app.mvflix_movies.catalog.domain.movie.MovieVisibility;
import com.gcorp.service.app.mvflix_movies.support.PostgresIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

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
            .updateDetails(movie.withMetadata(MovieMetadata.onlyTitle("New title")))
            .block();

    assertThat(updated).isNotNull();
    assertThat(updated.getTitle()).isEqualTo("New title");
    this.assertPersistedTitles(movie.getId().value(), "New title");
  }

  @Test
  void updateDetailsPersistsReclassificationAsOneAggregateState() {
    Movie movie = this.saveDraft("Dune")
        .linkProviderMetadata(new MovieMetadata(
            "Provider title", null, null, List.of(), null, null, null,
            List.of(), null, null, null, null, null, List.of(), 100L));
    Movie reclassified = movie.reclassifyAsOther(MovieMetadata.onlyTitle("Family recording"));

    Movie updated = this.movieRepository.updateDetails(reclassified).block();

    assertThat(updated).isNotNull();
    assertThat(updated.getKind()).isEqualTo(MediaKind.OTHER);
    assertThat(updated.getEnrichmentStatus()).isEqualTo(EnrichmentStatus.RAW);
    assertThat(updated.getMetadata().tmdbId()).isNull();
    this.assertPersistedTitles(movie.getId().value(), "Family recording");
  }

  @Test
  void updateEnrichmentSynchronizesCatalogTitle() {
    Movie movie = this.saveDraft("Local title");

    Movie updated =
        this.movieRepository
            .updateEnrichment(movie.linkProviderMetadata(new MovieMetadata(
                "Provider title", null, null, List.of(), null, null, null,
                List.of(), null, null, null, null, null, List.of(), 100L)))
            .block();

    assertThat(updated).isNotNull();
    assertThat(updated.getTitle()).isEqualTo("Provider title");
    assertThat(updated.getEnrichmentStatus()).isEqualTo(EnrichmentStatus.ENRICHED);
    this.assertPersistedTitles(movie.getId().value(), "Provider title");
  }

  @Test
  void updateVisibilityPreservesUploadedObjectId() {
    Movie movie = this.saveDraft("Dune");
    this.insertMedia(movie.getId().value(), 700L, "movies/visibility/video.mp4");

    Movie updated =
        this.movieRepository
            .updateVisibility(movie.getId(), MovieVisibility.PUBLIC)
            .block();

    assertThat(updated).isNotNull();
    assertThat(updated.getObjectId()).isEqualTo(700L);
  }

  @Test
  void replaceSharesPreservesUploadedObjectId() {
    Movie movie = this.saveDraft("Dune");
    this.insertMedia(movie.getId().value(), 701L, "movies/shares/video.mp4");

    Movie updated =
        this.movieRepository
            .replaceShares(movie.getId(), List.of("maria"))
            .block();

    assertThat(updated).isNotNull();
    assertThat(updated.getObjectId()).isEqualTo(701L);
    assertThat(updated.getSharedWith()).containsExactly("maria");
  }

  private Movie saveDraft(String title) {
    return this.movieRepository
        .save(Movie.createDraft("pepe", MovieMetadata.onlyTitle(title), MediaKind.MOVIE))
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
