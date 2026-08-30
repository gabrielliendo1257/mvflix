package com.gcorp.service.app.mvflix_movies.catalog.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.gcorp.service.app.mvflix_movies.catalog.domain.movie.MediaKind;
import com.gcorp.service.app.mvflix_movies.catalog.domain.movie.CatalogItem;
import com.gcorp.service.app.mvflix_movies.catalog.domain.movie.MovieMetadata;
import com.gcorp.service.app.mvflix_movies.catalog.domain.movie.CatalogItemRepository;
import com.gcorp.service.app.mvflix_movies.library.application.MediaAssetQueries;
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
 * La política de selección del asset reproducible debe ser ÚNICA: la puerta
 * de catálogo (proyección owned) y la puerta de playback (by-movie) tienen
 * que devolver el MISMO asset cuando coexisten versiones presentes y
 * ausentes. Sin este test, catálogo anuncia play=true y playback sirve el
 * archivo desaparecido.
 */
@ActiveProfiles("sandbox")
@SpringBootTest
class PlaybackAndCatalogAssetSelectionConsistencyTest extends PostgresIntegrationTest {

    @Autowired private CatalogItemRepository movieRepository;
    @Autowired private MediaAssetRepository mediaAssetRepository;
    @Autowired private CatalogViewSqlRepository catalogViewRepository;
    @Autowired private MediaAssetQueries mediaAssetQueries;
    @Autowired private DatabaseClient databaseClient;

    private long catalogMovieId;
    private long preferredAssetId;

    @BeforeEach
    void seedMixedVersions() {
        this.databaseClient.sql("DELETE FROM media_assets").fetch().rowsUpdated().block();
        this.databaseClient.sql("DELETE FROM media").fetch().rowsUpdated().block();
        this.databaseClient.sql("DELETE FROM movie_shares").fetch().rowsUpdated().block();
        this.databaseClient.sql("DELETE FROM catalog_items").fetch().rowsUpdated().block();

        CatalogItem movie = this.movieRepository.save(CatalogItem.fromLibraryAsset(
                "pepe", MovieMetadata.onlyTitle("Stalker"), MediaKind.MOVIE)).block();
        this.catalogMovieId = movie.getId().value();

        // id menor: versión vieja SIN archivo; id mayor: remux presente.
        // Flujo real: el insert nace UNIDENTIFIED y el vínculo lo pone
        // identifyIfUnidentified (ver CatalogSourcesCharacterizationTest).
        var oldCreated = this.mediaAssetRepository.save(
                MediaAsset.create(7L, new ScannedFile("stalker-old.mp4", 5L, "video/mp4"), "admin")).block();
        var oldIdentified = this.mediaAssetRepository.identifyIfUnidentified(
                oldCreated.getId(), CatalogItemId.of(this.catalogMovieId)).block();
        this.mediaAssetRepository.save(oldIdentified.markMissing()).block();
        var remuxCreated = this.mediaAssetRepository.save(
                MediaAsset.create(7L, new ScannedFile("stalker-remux.mp4", 9L, "video/mp4"), "admin")).block();
        this.preferredAssetId = this.mediaAssetRepository.identifyIfUnidentified(
                remuxCreated.getId(), CatalogItemId.of(this.catalogMovieId)).block()
                .getId().value();
    }

    @Test
    void catalogProjectionSelectsThePresentVersion() {
        StepVerifier.create(this.catalogViewRepository.page(new com.gcorp.service.app.mvflix_movies.catalog.application.CatalogReadQuery(
                        "pepe", 0, 25, "Stalker", null,
                        com.gcorp.service.app.mvflix_movies.catalog.application.CatalogReadQuery.SortField.UPDATED_AT,
                        false, false)))
                .assertNext(page -> {
                    assertThat(page.items()).hasSize(1);
                    var item = page.items().get(0);
                    assertThat(item.source()).isEqualTo("LOCAL");
                    assertThat(item.displayStatus()).isEqualTo("READY");
                    assertThat(item.assetPresent()).isTrue();
                    assertThat(item.assetId()).isEqualTo(this.preferredAssetId);
                })
                .verifyComplete();
    }

    @Test
    void playbackDoorResolvesTheSamePlayableVersion() {
        StepVerifier.create(this.mediaAssetQueries.findByCatalogItem(
                        CatalogItemId.of(this.catalogMovieId)))
                .assertNext(asset -> {
                    assertThat(asset.getId().value()).isEqualTo(this.preferredAssetId);
                    assertThat(asset.getPresent()).isTrue();
                    assertThat(asset.getRelativePath()).isEqualTo("stalker-remux.mp4");
                })
                .verifyComplete();
    }
}
