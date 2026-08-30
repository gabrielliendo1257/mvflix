package com.gcorp.service.app.mvflix_movies.catalog.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.gcorp.service.app.mvflix_movies.catalog.domain.movie.CatalogItemConflictException;
import com.gcorp.service.app.mvflix_movies.catalog.domain.movie.CatalogItemId;
import com.gcorp.service.app.mvflix_movies.catalog.domain.movie.CatalogItemRepository;
import com.gcorp.service.app.mvflix_movies.catalog.domain.movie.CatalogItemStatus;
import com.gcorp.service.app.mvflix_movies.support.PostgresIntegrationTest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.test.context.ActiveProfiles;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ActiveProfiles("sandbox")
@SpringBootTest
class MovieDeletionTransactionIntegrationTest extends PostgresIntegrationTest {

    @Autowired private MovieDeletionTransaction transaction;
    @Autowired private CatalogItemRepository movieRepository;
    @Autowired private CompleteMovieUseCase completeMovieUseCase;
    @Autowired private DatabaseClient databaseClient;

    @BeforeEach
    void cleanDatabase() {
        this.databaseClient.sql("DELETE FROM outbox_events").fetch().rowsUpdated().block();
        this.databaseClient.sql("DELETE FROM movie_shares").fetch().rowsUpdated().block();
        this.databaseClient.sql("DELETE FROM media").fetch().rowsUpdated().block();
        this.databaseClient.sql("DELETE FROM media_assets").fetch().rowsUpdated().block();
        this.databaseClient.sql("DELETE FROM movies").fetch().rowsUpdated().block();
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
                .sql("INSERT INTO media (movie_id, object_id, object_key) VALUES (:m, :o, :k)")
                .bind("m", movieId).bind("o", objectId).bind("k", objectKey)
                .fetch().rowsUpdated().block();
    }

    private void insertAsset(Long movieId, String path) {
        this.databaseClient
                .sql(
                        """
                        INSERT INTO media_assets (
                            library_id, relative_path, size, mime_type, status, movie_id,
                            discovered_by, present)
                        VALUES (7, :path, 1024, 'video/mp4', 'IDENTIFIED', :movie, 'admin', true)
                        """)
                .bind("path", path).bind("movie", movieId)
                .fetch().rowsUpdated().block();
    }

    private void insertShare(Long movieId, String user) {
        this.databaseClient
                .sql("INSERT INTO movie_shares (movie_id, shared_with) VALUES (:m, :u)")
                .bind("m", movieId).bind("u", user)
                .fetch().rowsUpdated().block();
    }

    private Mono<String> status(Long movieId) {
        return this.databaseClient
                .sql("SELECT status FROM movies WHERE id = :id")
                .bind("id", movieId)
                .map((row, metadata) -> row.get("status", String.class))
                .one();
    }

    private Mono<Long> count(String table, Long movieId) {
        return this.databaseClient
                .sql("SELECT COUNT(*) AS n FROM " + table + " WHERE movie_id = :m")
                .bind("m", movieId)
                .map((row, metadata) -> row.get("n", Long.class))
                .one();
    }

    private Mono<Long> outboxCount(Long movieId) {
        return this.databaseClient
                .sql("SELECT COUNT(*) AS n FROM outbox_events WHERE aggregate_id = :id")
                .bind("id", String.valueOf(movieId))
                .map((row, metadata) -> row.get("n", Long.class))
                .one();
    }

    @Test
    void markDeletingCasReadyToDeleting() {
        Long movie = this.insertMovie("READY");
        this.insertMedia(movie, 1L, "pepe/videos/mark.mp4");

        StepVerifier.create(this.transaction.requestDeletion(CatalogItemId.of(movie)))
                .assertNext(deleting ->
                        assertThat(deleting.getStatus()).isEqualTo(CatalogItemStatus.DELETING))
                .verifyComplete();

        StepVerifier.create(this.status(movie)).expectNext("DELETING").verifyComplete();
        StepVerifier.create(this.outboxCount(movie)).expectNext(1L).verifyComplete();

        StepVerifier.create(this.databaseClient
                        .sql("SELECT event_type, event_version, payload->'payload'->>'storageId' AS storage_id "
                                + "FROM outbox_events WHERE aggregate_id = :id")
                        .bind("id", String.valueOf(movie))
                        .map((row, metadata) -> java.util.List.of(
                                row.get("event_type", String.class),
                                String.valueOf(row.get("event_version", Integer.class)),
                                row.get("storage_id", String.class)))
                        .one())
                .assertNext(event -> {
                    assertThat(event).containsExactly("ManagedMediaDeletionRequested", "1", "1");
                })
                .verifyComplete();
    }

    @Test
    void secondRequestDoesNotDuplicateChanges() {
        Long movie = this.insertMovie("READY");
        this.insertMedia(movie, 2L, "pepe/videos/second.mp4");

        this.transaction.requestDeletion(CatalogItemId.of(movie)).block();
        StepVerifier.create(this.transaction.requestDeletion(CatalogItemId.of(movie)))
                .verifyComplete();

        StepVerifier.create(this.status(movie)).expectNext("DELETING").verifyComplete();
        StepVerifier.create(this.outboxCount(movie)).expectNext(1L).verifyComplete();
    }

    @Test
    void ensureDeletionRequestedBackfillsLegacyDeletingMovieWithoutDuplicates() {
        Long movie = this.insertMovie("DELETING");
        this.insertMedia(movie, 2L, "pepe/videos/legacy.mp4");

        StepVerifier.create(this.transaction.ensureDeletionRequested(CatalogItemId.of(movie)))
                .verifyComplete();
        StepVerifier.create(this.transaction.ensureDeletionRequested(CatalogItemId.of(movie)))
                .verifyComplete();

        StepVerifier.create(this.outboxCount(movie)).expectNext(1L).verifyComplete();
    }

    @Test
    void deleteIfDeletingDoesNotDeleteReadyMovie() {
        Long movie = this.insertMovie("READY");

        StepVerifier.create(this.movieRepository.deleteIfDeleting(CatalogItemId.of(movie)))
                .expectNext(false)
                .verifyComplete();

        StepVerifier.create(this.status(movie)).expectNext("READY").verifyComplete();
    }

    @Test
    void finalizeDeletionRemovesAssociations() {
        Long movie = this.insertMovie("READY");
        this.insertMedia(movie, 1L, "pepe/videos/dune.mp4");
        this.insertAsset(movie, "Movies/dune.mkv");
        this.insertShare(movie, "maria");
        this.transaction.requestDeletion(CatalogItemId.of(movie)).block();

        StepVerifier.create(this.transaction.finalizeDeletion(CatalogItemId.of(movie)))
                .verifyComplete();

        StepVerifier.create(this.movieRepository.findById(CatalogItemId.of(movie)))
                .verifyComplete();
        StepVerifier.create(this.count("media", movie)).expectNext(0L).verifyComplete();
        StepVerifier.create(this.count("movie_shares", movie)).expectNext(0L).verifyComplete();
        StepVerifier.create(this.databaseClient
                        .sql("SELECT status, movie_id, present FROM media_assets WHERE relative_path = :p")
                        .bind("p", "Movies/dune.mkv")
                        .map((row, meta) -> java.util.Map.of(
                                "status", String.valueOf(row.get("status", String.class)),
                                "movie", String.valueOf(row.get("movie_id", Long.class)),
                                "present", String.valueOf(row.get("present", Boolean.class))))
                        .one())
                .assertNext(asset -> {
                    assertThat(asset.get("status")).isEqualTo("UNIDENTIFIED");
                    assertThat(asset.get("movie")).isEqualTo("null");
                    assertThat(asset.get("present")).isEqualTo("true");
                })
                .verifyComplete();
    }

    @Test
    void managedDeletionDoesNotUnlinkAssetsWhenStorageIdDoesNotMatch() {
        Long movie = this.insertMovie("DELETING");
        this.insertMedia(movie, 1L, "pepe/videos/dune.mp4");
        this.insertAsset(movie, "Movies/dune.mkv");

        StepVerifier.create(this.transaction.finalizeManagedDeletion(CatalogItemId.of(movie), 999L))
                .expectError(IllegalStateException.class)
                .verify();

        StepVerifier.create(this.status(movie)).expectNext("DELETING").verifyComplete();
        StepVerifier.create(this.count("media_assets", movie)).expectNext(1L).verifyComplete();
    }

    @Test
    void managedDeletionDoesNotUnlinkAssetsWhenMovieIsNotDeleting() {
        Long movie = this.insertMovie("READY");
        this.insertMedia(movie, 1L, "pepe/videos/dune.mp4");
        this.insertAsset(movie, "Movies/dune.mkv");

        StepVerifier.create(this.transaction.finalizeManagedDeletion(CatalogItemId.of(movie), 1L))
                .expectError(IllegalStateException.class)
                .verify();

        StepVerifier.create(this.status(movie)).expectNext("READY").verifyComplete();
        StepVerifier.create(this.count("media_assets", movie)).expectNext(1L).verifyComplete();
    }

    @Test
    void managedDeletionOfAbsentMovieIsIdempotent() {
        StepVerifier.create(this.transaction.finalizeManagedDeletion(CatalogItemId.of(999999L), 1L))
                .verifyComplete();
    }

    @Test
    void completeMovieCannotReviveDeletingMovie() {
        Long movie = this.insertMovie("READY");
        this.insertMedia(movie, 3L, "pepe/videos/complete.mp4");
        this.transaction.requestDeletion(CatalogItemId.of(movie)).block();

        StepVerifier.create(
                        this.completeMovieUseCase.execute(CatalogItemId.of(movie), 700L, "pepe/videos/dune.mp4"))
                .expectError(CatalogItemConflictException.class)
                .verify();

        StepVerifier.create(this.status(movie)).expectNext("DELETING").verifyComplete();
    }

    @Test
    void findDeletingReturnsOnlyDeletingMovies() {
        Long deleting = this.insertMovie("READY");
        this.insertMedia(deleting, 4L, "pepe/videos/find.mp4");
        this.insertMovie("READY");
        this.insertMovie("DRAFT");
        this.transaction.requestDeletion(CatalogItemId.of(deleting)).block();

        StepVerifier.create(this.movieRepository.findDeleting(10))
                .assertNext(movie -> {
                    assertThat(movie.getId()).isEqualTo(CatalogItemId.of(deleting));
                    assertThat(movie.getStatus()).isEqualTo(CatalogItemStatus.DELETING);
                })
                .verifyComplete();
    }
}
