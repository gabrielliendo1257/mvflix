package com.gcorp.service.app.mvflix_movies.catalog.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.gcorp.service.app.mvflix_movies.catalog.domain.media.Media;
import com.gcorp.service.app.mvflix_movies.catalog.domain.media.MediaRepository;
import com.gcorp.service.app.mvflix_movies.catalog.domain.movie.MediaKind;
import com.gcorp.service.app.mvflix_movies.catalog.domain.movie.Movie;
import com.gcorp.service.app.mvflix_movies.catalog.domain.movie.MovieId;
import com.gcorp.service.app.mvflix_movies.catalog.domain.movie.MovieMetadata;
import com.gcorp.service.app.mvflix_movies.catalog.domain.movie.MovieRepository;
import com.gcorp.service.app.mvflix_movies.library.domain.CatalogItemId;
import com.gcorp.service.app.mvflix_movies.library.domain.MediaAsset;
import com.gcorp.service.app.mvflix_movies.library.domain.MediaAssetRepository;
import com.gcorp.service.app.mvflix_movies.library.domain.ScannedFile;
import com.gcorp.service.app.mvflix_movies.support.PostgresIntegrationTest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.test.context.ActiveProfiles;

import reactor.test.StepVerifier;

/**
 * Caracteriza las relaciones de DATOS sobre las que se apoyará la proyección
 * del catálogo owned (source MANAGED/LOCAL, sharedWithCount, campos de
 * display en el JSONB). Si algo de aquí cambia, la proyección se rompe y
 * este test debe fallar primero.
 */
@ActiveProfiles("sandbox")
@SpringBootTest
class CatalogSourcesCharacterizationTest extends PostgresIntegrationTest {

    @Autowired private MovieRepository movieRepository;
    @Autowired private MediaRepository mediaRepository;
    @Autowired private MediaAssetRepository mediaAssetRepository;
    @Autowired private DatabaseClient databaseClient;

    @BeforeEach
    void cleanDatabase() {
        this.databaseClient.sql("DELETE FROM media_assets").fetch().rowsUpdated().block();
        this.databaseClient.sql("DELETE FROM media").fetch().rowsUpdated().block();
        this.databaseClient.sql("DELETE FROM movie_shares").fetch().rowsUpdated().block();
        this.databaseClient.sql("DELETE FROM movies").fetch().rowsUpdated().block();
    }

    @Test
    void managedUploadIsRepresentedByAMediaRowWithObject() {
        Movie movie = this.movieRepository.save(Movie.createDraft(
                "pepe", MovieMetadata.onlyTitle("Coraline"), MediaKind.MOVIE)).block();

        this.mediaRepository.save(Media.create(movie.getId(), 42L, "pepe/coraline.mp4"))
                .as(StepVerifier::create)
                .expectNextCount(1)
                .verifyComplete();

        StepVerifier.create(this.mediaRepository.findByMovieId(movie.getId()))
                .assertNext(media -> assertThat(media.getObjectId()).isEqualTo(42L))
                .verifyComplete();
    }

    @Test
    void localLibraryMovieLinksAnIdentifiedPresentAsset() {
        Movie movie = this.movieRepository.save(Movie.fromLibraryAsset(
                "admin", MovieMetadata.onlyTitle("Dune"), MediaKind.MOVIE)).block();

        // Flujo real: el insert nace UNIDENTIFIED y la transacción de
        // identificación es quien pone el vínculo (identifyIfUnidentified).
        MediaAsset discovered = this.mediaAssetRepository
                .save(MediaAsset.create(
                        7L, new ScannedFile("Dune.mp4", 1024L, "video/mp4"), "admin"))
                .block();
        this.mediaAssetRepository
                .identifyIfUnidentified(discovered.getId(),
                        CatalogItemId.of(movie.getId().value()))
                .block();

        StepVerifier.create(this.mediaAssetRepository.findByCatalogItemId(
                        CatalogItemId.of(movie.getId().value())))
                .assertNext(asset -> {
                    assertThat(asset.isIdentified()).isTrue();
                    assertThat(asset.getPresent()).isTrue();
                    assertThat(asset.getCatalogItemId()).isEqualTo(
                            CatalogItemId.of(movie.getId().value()));
                })
                .verifyComplete();
    }

    @Test
    void sharesTableHoldsOneRowPerSharedUser() {
        Movie movie = this.movieRepository.save(Movie.createDraft(
                "pepe", MovieMetadata.onlyTitle("Alien"), MediaKind.MOVIE)).block();
        Long movieDbId = movie.getId().value();

        this.databaseClient.sql(
                "INSERT INTO movie_shares (movie_id, shared_with) VALUES (:m, :u)")
                .bind("m", movieDbId).bind("u", "maria").then()
                .then(this.databaseClient.sql(
                        "INSERT INTO movie_shares (movie_id, shared_with) VALUES (:m, :u)")
                        .bind("m", movieDbId).bind("u", "pedro").then())
                .block();

        this.databaseClient.sql(
                "SELECT COUNT(*) AS n FROM movie_shares WHERE movie_id = :m")
                .bind("m", movieDbId)
                .map((row, meta) -> row.get("n", Long.class))
                .one()
                .as(StepVerifier::create)
                .assertNext(count -> assertThat(count).isEqualTo(2L))
                .verifyComplete();
    }

    @Test
    void metadataJsonbCarriesTheDisplayFieldsTheGridNeeds() {
        this.movieRepository.save(Movie.createDraft(
                "pepe",
                new MovieMetadata("Coraline", null, 2009, null, null,
                        "1h 40m", null, null, null, "/coraline.jpg", null,
                        null, null, null, null),
                MediaKind.MOVIE))
                .block();

        this.databaseClient.sql("""
                SELECT m.metadata->>'posterPath' AS poster,
                       m.metadata->>'year' AS year,
                       m.metadata->>'duration' AS duration
                FROM movies m WHERE m.title = 'Coraline'
                """)
                .map((row, meta) -> java.util.Map.of(
                        "poster", String.valueOf(row.get("poster", String.class)),
                        "year", String.valueOf(row.get("year", String.class)),
                        "duration", String.valueOf(row.get("duration", String.class))))
                .one()
                .as(StepVerifier::create)
                .assertNext(fields -> {
                    assertThat(fields.get("poster")).isEqualTo("/coraline.jpg");
                    assertThat(fields.get("year")).isEqualTo("2009");
                    assertThat(fields.get("duration")).isEqualTo("1h 40m");
                })
                .verifyComplete();
    }
}
