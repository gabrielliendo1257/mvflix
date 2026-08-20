package com.gcorp.service.app.mvflix_movies.domain.movie;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

/**
 * Las fábricas codifican las dos clases de nacimiento de una película (DRAFT de
 * upload vs READY de biblioteca) y sus invariantes; los predicados explicitan la
 * dualidad de READY (subido vs respaldado por archivo local).
 */
class MovieFactoryTest {

    private static final MovieMetadata TITLE = MovieMetadata.onlyTitle("Dune");

    @Test
    void createDraftNacePrivadaYEnDraftSinObjeto() {
        Movie draft = Movie.createDraft("Javier", TITLE);

        assertThat(draft.getStatus()).isEqualTo(MovieStatus.DRAFT);
        assertThat(draft.getEnrichmentStatus()).isEqualTo(EnrichmentStatus.RAW);
        assertThat(draft.getVisibility()).isEqualTo(MovieVisibility.PRIVATE);
        assertThat(draft.getObjectId()).isNull();
        assertThat(draft.getSharedWith()).isEmpty();
        assertThat(draft.getTitle()).isEqualTo("Dune");
        assertThat(draft.isLibraryBacked()).isFalse();
        assertThat(draft.isUploaded()).isFalse();
    }

    @Test
    void fromLibraryAssetNaceReadyRespaldadaPorArchivoLocal() {
        Movie movie = Movie.fromLibraryAsset("Javier", TITLE);

        assertThat(movie.getStatus()).isEqualTo(MovieStatus.READY);
        assertThat(movie.getObjectId()).isNull();
        assertThat(movie.isLibraryBacked()).isTrue();
        assertThat(movie.isUploaded()).isFalse();
    }

    @Test
    void completeProduceReadySubidoAlStorage() {
        Movie uploaded = Movie.createDraft("Javier", TITLE).complete(42L);

        assertThat(uploaded.getStatus()).isEqualTo(MovieStatus.READY);
        assertThat(uploaded.getObjectId()).isEqualTo(42L);
        assertThat(uploaded.isUploaded()).isTrue();
        assertThat(uploaded.isLibraryBacked()).isFalse();
    }

    @Test
    void tituloEnBlancoSeRechaza() {
        assertThatThrownBy(() -> Movie.createDraft("Javier", MovieMetadata.onlyTitle(" ")))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Movie.fromLibraryAsset("Javier", MovieMetadata.onlyTitle("")))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void duenoEnBlancoSeRechaza() {
        assertThatThrownBy(() -> Movie.createDraft(" ", TITLE))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Movie.fromLibraryAsset(null, TITLE))
                .isInstanceOf(IllegalArgumentException.class);
    }
}