package com.gcorp.service.app.mvflix_movies.catalog.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.gcorp.service.app.mvflix_movies.catalog.application.CatalogItemView;
import com.gcorp.service.app.mvflix_movies.catalog.application.CatalogPageView;
import com.gcorp.service.app.mvflix_movies.catalog.application.CatalogReadQuery;
import com.gcorp.service.app.mvflix_movies.catalog.domain.media.ManagedMediaAsset;
import com.gcorp.service.app.mvflix_movies.catalog.domain.media.MediaRepository;
import com.gcorp.service.app.mvflix_movies.catalog.domain.movie.MediaKind;
import com.gcorp.service.app.mvflix_movies.catalog.domain.movie.CatalogItem;
import com.gcorp.service.app.mvflix_movies.catalog.domain.movie.MovieMetadata;
import com.gcorp.service.app.mvflix_movies.catalog.domain.movie.CatalogItemRepository;
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
    @Autowired private CatalogItemRepository movieRepository;
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
        this.databaseClient.sql("DELETE FROM catalog_items").fetch().rowsUpdated().block();

        // MANAGED: READY con metadata completa, objeto en media, 2 compartidos.
        CatalogItem managed = this.movieRepository.save(new CatalogItem(
                null, "pepe", "Coraline", com.gcorp.service.app.mvflix_movies.catalog.domain.movie.CatalogItemStatus.READY,
                com.gcorp.service.app.mvflix_movies.catalog.domain.movie.EnrichmentStatus.ENRICHED,
                null,
                new MovieMetadata("Coraline", null, 2009, null, null, "1h 40m",
                        null, null, null, "/coraline.jpg", null, null, null, null, 57892L),
                com.gcorp.service.app.mvflix_movies.catalog.domain.movie.CatalogItemVisibility.PRIVATE,
                java.util.Set.of(), MediaKind.MOVIE)).block();
        this.managedId = movieRepository.findById(managed.getId()).block().getId().value();
        this.mediaRepository.save(ManagedMediaAsset.create(managed.getId(), 42L, "k")).block();
        share(managed.getId().value(), "maria");
        share(managed.getId().value(), "pedro");

        // LOCAL: READY de biblioteca con asset identificado.
        CatalogItem local = this.movieRepository.save(CatalogItem.fromLibraryAsset(
                "pepe", MovieMetadata.onlyTitle("Alien"), MediaKind.MOVIE)).block();
        this.localId = local.getId().value();
        MediaAsset discovered = this.mediaAssetRepository.save(
                MediaAsset.create(7L, new ScannedFile("Alien.mp4", 10L, "video/mp4"), "admin")).block();
        this.mediaAssetRepository.identifyIfUnidentified(
                discovered.getId(), CatalogItemId.of(this.localId)).block();

        // DRAFT sin contenido: needsAttention.
        CatalogItem draft = this.movieRepository.save(CatalogItem.createDraft(
                "pepe", MovieMetadata.onlyTitle("Beta"), MediaKind.MOVIE)).block();
        this.draftId = draft.getId().value();
    }

    private void share(long movieId, String user) {
        this.databaseClient.sql(
                "INSERT INTO movie_shares (catalog_item_id, shared_with) VALUES (:m, :u)")
                .bind("m", movieId).bind("u", user).then().block();
    }

    private CatalogPageView page(String search, String status) {
        return this.repository.page(new CatalogReadQuery(
                "pepe", 0, 25, search, status,
                CatalogReadQuery.SortField.UPDATED_AT, false, false)).block();
    }

    private CatalogPageView pageAsAdmin(String search, String status) {
        return this.repository.page(new CatalogReadQuery(
                "pepe", 0, 25, search, status,
                CatalogReadQuery.SortField.UPDATED_AT, false, true)).block();
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
                "pepe", 0, 25, null, null, CatalogReadQuery.SortField.TITLE, true, false)).block();

        assertThat(view.items()).extracting(CatalogItemView::title)
                .containsExactly("Alien", "Beta", "Coraline");
    }

    @Test
    void paginationSlicesWithoutDuplicatingJoinedRows() {
        Flux.range(0, 30)
                .flatMap(i -> this.movieRepository.save(CatalogItem.createDraft(
                        "pepe", MovieMetadata.onlyTitle("Bulk " + i), MediaKind.MOVIE)))
                .collectList().block();

        Mono<CatalogPageView> paged = this.repository.page(new CatalogReadQuery(
                "pepe", 1, 25, null, null, CatalogReadQuery.SortField.UPDATED_AT, false, false));
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
        this.movieRepository.save(CatalogItem.fromLibraryAsset(
                "maria", MovieMetadata.onlyTitle("Ajena"), MediaKind.MOVIE)).block();

        CatalogPageView view = page(null, null);
        assertThat(view.items()).noneMatch(i -> "Ajena".equals(i.title()));
    }

    /** Inserta un asset identificado directo a la movie (multi-versión permitida). */
    private MediaAsset addIdentifiedAsset(long movieId, String path, boolean present) {
        MediaAsset created = this.mediaAssetRepository.save(
                MediaAsset.create(7L, new ScannedFile(path, 10L, "video/mp4"), "admin")).block();
        MediaAsset identified = created.identify(CatalogItemId.of(movieId));
        return this.mediaAssetRepository.save(
                present ? identified : identified.markMissing()).block();
    }

    private void setMissing(MediaAsset asset) {
        this.mediaAssetRepository.save(
                this.mediaAssetRepository.findById(asset.getId()).block().markMissing()).block();
    }

    @Test
    void multipleIdentifiedAssetsPrefersThePlayableOne() {
        // Versión vieja sin archivo (id menor) + versión presente (id mayor):
        // el asset reproducible preferido es el PRESENTE, no el primero.
        CatalogItem movie = this.movieRepository.save(CatalogItem.fromLibraryAsset(
                "pepe", MovieMetadata.onlyTitle("Stalker"), MediaKind.MOVIE)).block();
        long missingId = addIdentifiedAsset(movie.getId().value(),
                "stalker-old.mp4", false).getId().value();
        long presentId = addIdentifiedAsset(movie.getId().value(),
                "stalker-remux.mp4", true).getId().value();

        CatalogPageView view = page("Stalker", null);

        var item = view.items().get(0);
        assertThat(item.assetId()).isEqualTo(presentId);
        assertThat(item.assetId()).isNotEqualTo(missingId);
        assertThat(item.assetPresent()).isTrue();
        assertThat(item.displayStatus()).isEqualTo("READY");
        assertThat(view.summary().ready()).isEqualTo(1);
    }

    @Test
    void whenNoVersionIsPresentTheOldestMissingOneIsReported() {
        CatalogItem movie = this.movieRepository.save(CatalogItem.fromLibraryAsset(
                "pepe", MovieMetadata.onlyTitle("Solaris"), MediaKind.MOVIE)).block();
        var first = addIdentifiedAsset(movie.getId().value(), "solaris-a.mp4", false);
        addIdentifiedAsset(movie.getId().value(), "solaris-b.mp4", false);
        setMissing(first); // asegura estado MISSING persistido para ambos

        CatalogPageView view = page("Solaris", null);

        var item = view.items().get(0);
        assertThat(item.displayStatus()).isEqualTo("MISSING");
        assertThat(item.assetPresent()).isFalse();
        assertThat(item.assetId()).isEqualTo(first.getId().value());
    }

    @Test
    void movieWithMultipleMediaRowsAppearsExactlyOnceAndPaginationStaysConsistent() {
        // Segunda fila de media para la misma película (trailer futuro, etc.).
        this.mediaRepository.save(ManagedMediaAsset.create(
                com.gcorp.service.app.mvflix_movies.catalog.domain.movie.CatalogItemId.of(this.managedId),
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

    @Test
    void unidentifiedAssetsAppearWithStableAssetKey() {
        // Asset sin identificar del admin + otro del usuario pepe.
        this.mediaAssetRepository.save(
                MediaAsset.create(7L, new ScannedFile("Movies/video_123.mkv", 10L, "video/x-matroska"), "admin")).block();
        this.mediaAssetRepository.save(
                MediaAsset.create(7L, new ScannedFile("Movies/mi_clip.mp4", 10L, "video/mp4"), "pepe")).block();

        // No-admin ve SOLO sus descubrimientos, nunca los del admin.
        CatalogPageView own = page(null, null);
        assertThat(own.items()).anyMatch(i ->
                "ASSET".equals(i.key().type()) && "mi_clip.mp4".equals(i.title()));
        assertThat(own.items()).noneMatch(i -> "video_123.mkv".equals(i.title()));

        // Admin ve todos los assets sin identificar.
        CatalogPageView admin = pageAsAdmin(null, null);
        var asset = admin.items().stream()
                .filter(i -> "video_123.mkv".equals(i.title())).findFirst().orElseThrow();
        assertThat(asset.key().type()).isEqualTo("ASSET");
        assertThat(asset.key().id()).isNotNull();
        assertThat(asset.mediaId()).isNull();
        assertThat(asset.assetId()).isEqualTo(asset.key().id());
        assertThat(asset.displayStatus()).isEqualTo("UNIDENTIFIED");
        assertThat(asset.source()).isEqualTo(CatalogItemView.Source.LOCAL.name());
        assertThat(asset.assetPresent()).isTrue();
        assertThat(asset.kind()).isNull();
        assertThat(asset.status()).isNull();

        // Filtrar por UNIDENTIFIED devuelve solo assets.
        CatalogPageView unidentified = pageAsAdmin(null, "UNIDENTIFIED");
        assertThat(unidentified.items()).isNotEmpty();
        assertThat(unidentified.items())
                .allMatch(i -> "UNIDENTIFIED".equals(i.displayStatus())
                        && "ASSET".equals(i.key().type()));
    }

    @Test
    void missingUnidentifiedAssetSurfacesAsMissingNotIdentifiable() {
        // Asset sin identificar que el scan ya no encuentra: present=false.
        MediaAsset created = this.mediaAssetRepository.save(
                MediaAsset.create(7L, new ScannedFile("Movies/gone.mkv", 10L, "video/x-matroska"), "admin")).block();
        this.mediaAssetRepository.save(created.markMissing()).block();

        CatalogPageView view = pageAsAdmin("gone", null);

        var item = view.items().get(0);
        assertThat(item.key().type()).isEqualTo("ASSET");
        assertThat(item.assetPresent()).isFalse();
        assertThat(item.displayStatus()).isEqualTo("MISSING");
        assertThat(item.source()).isEqualTo(CatalogItemView.Source.LOCAL.name());

        // MISSING es parte del vocabulario operacional del filtro.
        CatalogPageView missing = pageAsAdmin(null, "MISSING");
        assertThat(missing.items()).isNotEmpty();
        assertThat(missing.items())
                .allMatch(i -> "MISSING".equals(i.displayStatus()));
    }
}
