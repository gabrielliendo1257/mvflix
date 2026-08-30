package com.gcorp.service.app.mvflix_movies.catalog.domain.movie;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

import java.util.List;

/**
 * Las fábricas codifican las dos clases de nacimiento de un item (DRAFT de upload
 * vs READY de biblioteca), su tipo de contenido (MediaKind) y sus invariantes; los
 * predicados explicitan la dualidad de READY (subido vs respaldado por archivo local).
 */
class MovieFactoryTest {

    private static final MovieMetadata TITLE = MovieMetadata.onlyTitle("Dune");

    @Test
    void createDraftNacePrivadaYEnDraftSinObjeto() {
        CatalogItem draft = CatalogItem.createDraft("Javier", TITLE, MediaKind.MOVIE);

        assertThat(draft.getStatus()).isEqualTo(CatalogItemStatus.DRAFT);
        assertThat(draft.getEnrichmentStatus()).isEqualTo(EnrichmentStatus.RAW);
        assertThat(draft.getVisibility()).isEqualTo(CatalogItemVisibility.PRIVATE);
        assertThat(draft.getSharedWith()).isEmpty();
        assertThat(draft.getTitle()).isEqualTo("Dune");
        assertThat(draft.getKind()).isEqualTo(MediaKind.MOVIE);
        assertThat(draft.isMovie()).isTrue();
    }

    @Test
    void fromLibraryAssetNaceReadyRespaldadaPorArchivoLocal() {
        CatalogItem movie = CatalogItem.fromLibraryAsset("Javier", TITLE, MediaKind.MOVIE);

        assertThat(movie.getStatus()).isEqualTo(CatalogItemStatus.READY);
    }

    @Test
    void completeProduceReadySubidoAlStorage() {
        CatalogItem uploaded = CatalogItem.createDraft("Javier", TITLE, MediaKind.MOVIE).complete();

        assertThat(uploaded.getStatus()).isEqualTo(CatalogItemStatus.READY);
    }

    @Test
    void otherNaceConSoloTituloYSinProveedor() {
        CatalogItem clip = CatalogItem.fromLibraryAsset("Javier", TITLE, MediaKind.VIDEO);

        assertThat(clip.getKind()).isEqualTo(MediaKind.VIDEO);
        assertThat(clip.isMovie()).isFalse();
        assertThat(clip.getMetadata()).isInstanceOf(VideoMetadata.class);
        assertThat(clip.getStatus()).isEqualTo(CatalogItemStatus.READY);
    }

    @Test
    void kindNullSeResuelveAMovie() {
        CatalogItem draft = CatalogItem.createDraft("Javier", TITLE, null);

        assertThat(draft.getKind()).isEqualTo(MediaKind.MOVIE);
        assertThat(draft.isMovie()).isTrue();
    }

    @Test
    void tituloEnBlancoSeRechaza() {
        assertThatThrownBy(() ->
                CatalogItem.createDraft("Javier", MovieMetadata.onlyTitle(" "), MediaKind.MOVIE))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() ->
                CatalogItem.fromLibraryAsset("Javier", MovieMetadata.onlyTitle(""), MediaKind.MOVIE))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void duenoEnBlancoSeRechaza() {
        assertThatThrownBy(() -> CatalogItem.createDraft(" ", TITLE, MediaKind.MOVIE))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> CatalogItem.fromLibraryAsset(null, TITLE, MediaKind.MOVIE))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void withoutProviderLimpiaLosDatosDelProveedorYConservaLoManual() {
        MovieMetadata enriched = new MovieMetadata(
                "Dune", "Dune", 2021, List.of("Sci-Fi"), 7.9, "2h 35m",
                "Denis Villeneuve", List.of("Timothée Chalamet"), "Overview",
                "/poster.jpg", "2021-10-22", "USA", "English", List.of("Oscar"), 438631L);

        MovieMetadata unlinked = enriched.withoutProvider();

        assertThat(unlinked.tmdbId()).isNull();
        assertThat(unlinked.posterPath()).isNull();
        assertThat(unlinked.popularity()).isNull();
        assertThat(unlinked.title()).isEqualTo("Dune");
        assertThat(unlinked.overview()).isEqualTo("Overview");
    }
}
