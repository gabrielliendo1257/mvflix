package com.gcorp.service.app.mvflix_movies.catalog.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.gcorp.service.app.mvflix_movies.catalog.application.CatalogItemView;
import com.gcorp.service.app.mvflix_movies.catalog.application.CatalogPageView;
import com.gcorp.service.app.mvflix_movies.catalog.application.CatalogReadQuery;
import com.gcorp.service.app.mvflix_movies.catalog.domain.media.Media;
import com.gcorp.service.app.mvflix_movies.catalog.domain.media.MediaRepository;
import com.gcorp.service.app.mvflix_movies.catalog.domain.movie.MediaKind;
import com.gcorp.service.app.mvflix_movies.catalog.domain.movie.Movie;
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

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@ActiveProfiles("sandbox")
@SpringBootTest
class CatalogViewSqlRepositoryTest extends PostgresIntegrationTest {

    @Autowired private CatalogViewSqlRepository repository;
    @Autowired private MovieRepository movieRepository;
    @Autowired private MediaRepository mediaRepository;
    @Autowired private MediaAssetRepository mediaAssetRepository;
    @Autowired private DatabaseClient databaseClient;

    private long managedId;
    private long localId;
    private long draftId;

    @BeforeEach
    void seed() {
        this.databaseClient.sql("DELETE FROM media_assets").fetch().rowsUpdated().block();
        this.databaseClient.sql("DELETE FROM media").fetch().rowsUpdated().block();
        this.databaseClient.sql("DELETE FROM movie_shares").fetch().rowsUpdated().block();
        this.databaseClient.sql("DELETE FROM movies").fetch().rowsUpdated().block();

        // MANAGED: READY con metadata completa, objeto en media, 2 compartidos.
        Movie managed = this.movieRepository.save(new Movie(
                null, "pepe", "Coraline", com.gcorp.service.app.mvflix_movies.catalog.domain.movie.MovieStatus.READY,
                com.gcorp.service.app.mvflix_movies.catalog.domain.movie.EnrichmentStatus.ENRICHED,
                null,
                new MovieMetadata("Coraline", null, 2009, null, null, "1h 40m",
                        null, null, null, "/coraline.jpg", null, null, null, null, 57892L),
                com.gcorp.service.app.mvflix_movies.catalog.domain.movie.MovieVisibility.PRIVATE,
                java.util.Set.of(), MediaKind.MOVIE)).block();
        this.managedId = movieRepository.findById(managed.getId()).block().getId().value();
        this.mediaRepository.save(Media.create(managed.getId(), 42L, "k")).block();
        share(managed.getId().value(), "maria");
        share(managed.getId().value(), "pedro");

        // LOCAL: READY de biblioteca con asset identificado.
        Movie local = this.movieRepository.save(Movie.fromLibraryAsset(
                "pepe", MovieMetadata.onlyTitle("Alien"), MediaKind.MOVIE)).block();
        this.localId = local.getId().value();
        MediaAsset discovered = this.mediaAssetRepository.save(
                MediaAsset.create(7L, new ScannedFile("Alien.mp4", 10L, "video/mp4"), "admin")).block();
        this.mediaAssetRepository.identifyIfUnidentified(
                discovered.getId(), CatalogItemId.of(this.localId)).block();

        // DRAFT sin contenido: needsAttention.
        Movie draft = this.movieRepository.save(Movie.createDraft(
                "pepe", MovieMetadata.onlyTitle("Beta"), MediaKind.MOVIE)).block();
        this.draftId = draft.getId().value();
    }

    private void share(long movieId, String user) {
        this.databaseClient.sql(
                "INSERT INTO movie_shares (movie_id, shared_with) VALUES (:m, :u)")
                .bind("m", movieId).bind("u", user).then().block();
    }

    private CatalogPageView page(String search, String status) {
        return this.repository.page(new CatalogReadQuery(
                "pepe", 0, 25, search, status,
                CatalogReadQuery.SortField.UPDATED_AT, false)).block();
    }

    @Test
    void projectsSourcesSummaryAndProviderStatus() {
        CatalogPageView view = page(null, null);

        assertThat(view.total()).isEqualTo(3);
        assertThat(view.totalPages()).isEqualTo(1);
        assertThat(view.summary().ready()).isEqualTo(2);
        assertThat(view.summary().needsAttention()).isEqualTo(1);

        var coraline = view.items().stream()
                .filter(i -> "Coraline".equals(i.title())).findFirst().orElseThrow();
        assertThat(coraline.source()).isEqualTo(CatalogItemView.Source.MANAGED.name());
        assertThat(coraline.displayStatus()).isEqualTo("READY");
        assertThat(coraline.sharedWithCount()).isEqualTo(2);
        assertThat(coraline.providerStatus()).isEqualTo(CatalogItemView.ProviderStatus.LINKED.name());
        assertThat(coraline.year()).isEqualTo(2009);
        assertThat(coraline.duration()).isEqualTo("1h 40m");
        assertThat(coraline.key().type()).isEqualTo("MEDIA");

        var alien = view.items().stream()
                .filter(i -> "Alien".equals(i.title())).findFirst().orElseThrow();
        assertThat(alien.source()).isEqualTo(CatalogItemView.Source.LOCAL.name());
        assertThat(alien.assetId()).isNotNull();
        assertThat(alien.providerStatus()).isEqualTo(CatalogItemView.ProviderStatus.NONE.name());
    }

    @Test
    void searchFiltersByTitleCaseInsensitive() {
        CatalogPageView view = page("coral", null);
        assertThat(view.items()).extracting(CatalogItemView::title).containsExactly("Coraline");
    }

    @Test
    void statusFilterOnlyKeepsMatchingRows() {
        // Vocabulario operacional: PROCESSING (no DRAFT), MISSING, ATTENTION...
        CatalogPageView processing = page(null, "PROCESSING");
        assertThat(processing.total()).isEqualTo(1);
        assertThat(processing.summary().needsAttention()).isEqualTo(1);
        assertThat(processing.items()).extracting(CatalogItemView::title).containsExactly("Beta");

        CatalogPageView ready = page(null, "READY");
        assertThat(ready.total()).isEqualTo(2);

        // Un valor fuera del vocabulario operacional no filtra nada:
        // la normalización vive en el use case, el repositorio es literal.
        CatalogPageView unknown = page(null, "NO_EXISTE");
        assertThat(unknown.total()).isZero();
    }

    @Test
    void titleSortAscUsesWhitelistedColumn() {
        CatalogPageView view = this.repository.page(new CatalogReadQuery(
                "pepe", 0, 25, null, null, CatalogReadQuery.SortField.TITLE, true)).block();

        assertThat(view.items()).extracting(CatalogItemView::title)
                .containsExactly("Alien", "Beta", "Coraline");
    }

    @Test
    void paginationSlicesWithoutDuplicatingJoinedRows() {
        Flux.range(0, 30)
                .flatMap(i -> this.movieRepository.save(Movie.createDraft(
                        "pepe", MovieMetadata.onlyTitle("Bulk " + i), MediaKind.MOVIE)))
                .collectList().block();

        Mono<CatalogPageView> paged = this.repository.page(new CatalogReadQuery(
                "pepe", 1, 25, null, null, CatalogReadQuery.SortField.UPDATED_AT, false));
        CatalogPageView second = paged.block();

        assertThat(second.page()).isEqualTo(1);
        assertThat(second.size()).isEqualTo(25);
        assertThat(second.items().size()).isLessThanOrEqualTo(25);
        // total del filtro global, no solo la página
        assertThat(second.total()).isEqualTo(33);
        assertThat(second.totalPages()).isEqualTo(2);
    }

    @Test
    void otherOwnersContentNeverAppears() {
        this.movieRepository.save(Movie.fromLibraryAsset(
                "maria", MovieMetadata.onlyTitle("Ajena"), MediaKind.MOVIE)).block();

        CatalogPageView view = page(null, null);
        assertThat(view.items()).noneMatch(i -> "Ajena".equals(i.title()));
    }

    @Test
    void movieWithMultipleMediaRowsAppearsExactlyOnceAndPaginationStaysConsistent() {
        // Segunda fila de media para la misma película (trailer futuro, etc.).
        this.mediaRepository.save(Media.create(
                com.gcorp.service.app.mvflix_movies.catalog.domain.movie.MovieId.of(this.managedId),
                43L, "k2")).block();

        CatalogPageView view = page("Coraline", null);

        assertThat(view.total()).isEqualTo(1);
        assertThat(view.items()).hasSize(1);
        assertThat(view.totalPages()).isEqualTo(1);
        assertThat(view.summary().total()).isEqualTo(1);
        assertThat(view.items().get(0).source())
                .isEqualTo(CatalogItemView.Source.MANAGED.name());
    }

    @Test
    void dualSourceMovieSurfacesAsInvalidAttentionAndLeavesReady() {
        // Coraline ya es MANAGED; al identificarle además un asset local el
        // catálogo no elige en silencio: marca INVALID/ATTENTION y la saca de
        // ready (mismo criterio que playback: violación de contrato).
        MediaAsset discovered = this.mediaAssetRepository.save(
                MediaAsset.create(9L, new ScannedFile("Coraline.mp4", 10L, "video/mp4"), "admin")).block();
        this.mediaAssetRepository.identifyIfUnidentified(
                discovered.getId(), CatalogItemId.of(this.managedId)).block();

        CatalogPageView view = page("Coraline", null);

        var item = view.items().get(0);
        assertThat(item.source()).isEqualTo(CatalogItemView.Source.INVALID.name());
        assertThat(item.displayStatus()).isEqualTo("ATTENTION");

        CatalogPageView whole = page(null, null);
        assertThat(whole.summary().total()).isEqualTo(3);
        assertThat(whole.summary().ready()).isEqualTo(1);
        assertThat(whole.summary().needsAttention()).isEqualTo(2);
    }

    @Test
    void missingLocalAssetDerivesMissingAttentionStatus() {
        MediaAsset alien = this.mediaAssetRepository.findByCatalogItemId(
                CatalogItemId.of(this.localId)).block();
        this.mediaAssetRepository.save(alien.markMissing()).block();

        CatalogPageView view = page("Alien", null);

        var item = view.items().get(0);
        assertThat(item.source()).isEqualTo(CatalogItemView.Source.LOCAL.name());
        assertThat(item.assetPresent()).isFalse();
        assertThat(item.displayStatus()).isEqualTo("MISSING");

        // El resumen global deja de contarla como lista y la pasa a atención.
        CatalogPageView whole = page(null, null);
        assertThat(whole.summary().total()).isEqualTo(3);
        assertThat(whole.summary().ready()).isEqualTo(1);
        assertThat(whole.summary().needsAttention()).isEqualTo(2);
    }
}
