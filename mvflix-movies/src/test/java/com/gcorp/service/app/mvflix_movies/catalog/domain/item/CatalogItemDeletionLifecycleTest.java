package com.gcorp.service.app.mvflix_movies.catalog.domain.item;

import com.gcorp.service.app.mvflix_movies.catalog.domain.movie.EnrichmentStatus;
import com.gcorp.service.app.mvflix_movies.catalog.domain.movie.MovieMetadata;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

import java.util.Set;

/**
 * Ciclo de vida DELETING: una vez iniciado el borrado durable, la media es un
 * estado terminal operativo — no se completa, edita, cambia de acceso ni
 * reproduce. requestDeletion es idempotente. Test puro, sin Spring.
 */
class CatalogItemDeletionLifecycleTest {

    private static CatalogItem movie(CatalogItemStatus status) {
        return new CatalogItem(
                CatalogItemId.of(1L), "Javier", "Dune", status, EnrichmentStatus.ENRICHED,
                42L, MovieMetadata.onlyTitle("Dune"), CatalogItemVisibility.PRIVATE,
                Set.of(), CatalogItemKind.MOVIE);
    }

    @Test
    void requestDeletionMovesReadyToDeleting() {
        CatalogItem deleting = movie(CatalogItemStatus.READY).requestDeletion();

        assertThat(deleting.getStatus()).isEqualTo(CatalogItemStatus.DELETING);
        assertThat(deleting.isDeleting()).isTrue();
    }

    @Test
    void requestDeletionIsIdempotent() {
        CatalogItem alreadyDeleting = movie(CatalogItemStatus.DELETING);

        CatalogItem result = alreadyDeleting.requestDeletion();

        assertThat(result.getStatus()).isEqualTo(CatalogItemStatus.DELETING);
    }

    @Test
    void deletingMovieCannotComplete() {
        CatalogItem deleting = movie(CatalogItemStatus.DELETING);

        assertThatThrownBy(deleting::complete)
                .isInstanceOf(CatalogItemConflictException.class)
                .hasMessageContaining("DELETING");
    }

    @Test
    void deletingMovieCannotBeEdited() {
        CatalogItem deleting = movie(CatalogItemStatus.DELETING);

        assertThatThrownBy(() -> deleting.withMetadata(MovieMetadata.onlyTitle("Otro")))
                .isInstanceOf(CatalogItemConflictException.class);
    }

    @Test
    void deletingMovieCannotChangeVisibility() {
        CatalogItem deleting = movie(CatalogItemStatus.DELETING);

        assertThatThrownBy(() -> deleting.withVisibility(CatalogItemVisibility.PUBLIC))
                .isInstanceOf(CatalogItemConflictException.class);
    }

    @Test
    void deletingMovieCannotChangeShares() {
        CatalogItem deleting = movie(CatalogItemStatus.DELETING);

        assertThatThrownBy(() -> deleting.withSharedWith(Set.of("Maria")))
                .isInstanceOf(CatalogItemConflictException.class);
    }

    @Test
    void deletingMovieCannotChangeAccess() {
        CatalogItem deleting = movie(CatalogItemStatus.DELETING);

        assertThatThrownBy(() -> deleting.withAccess(CatalogItemVisibility.PRIVATE, Set.of()))
                .isInstanceOf(CatalogItemConflictException.class);
    }

    @Test
    void deletingMovieCannotLinkOrUnlinkProvider() {
        CatalogItem deleting = movie(CatalogItemStatus.DELETING);

        assertThatThrownBy(() -> deleting.linkProviderMetadata(
                new MovieMetadata("Dune", null, 2021, null, null, null,
                        null, null, null, null, null, null, null, null, 438631L)))
                .isInstanceOf(CatalogItemConflictException.class);
        assertThatThrownBy(deleting::unlinkProvider)
                .isInstanceOf(CatalogItemConflictException.class);
    }

    @Test
    void deletingMovieCannotBeReclassified() {
        CatalogItem deleting = movie(CatalogItemStatus.DELETING);

        assertThatThrownBy(() -> deleting.reclassifyAsVideo(MovieMetadata.onlyTitle("Clip")))
                .isInstanceOf(CatalogItemConflictException.class);
        assertThatThrownBy(deleting::reclassifyAsMovie)
                .isInstanceOf(CatalogItemConflictException.class);
    }

    @Test
    void deletingIsNotPlayable() {
        // El playback y el catálogo derivan la reproducibilidad del status:
        // DELETING no es READY, así que no se reproduce.
        CatalogItem deleting = movie(CatalogItemStatus.DELETING);

        assertThat(deleting.getStatus()).isNotEqualTo(CatalogItemStatus.READY);
    }

    @Test
    void nonDeletingMovieStillTransitionsNormally() {
        CatalogItem ready = movie(CatalogItemStatus.READY);
        CatalogItem draft = movie(CatalogItemStatus.DRAFT);

        assertThat(ready.withVisibility(CatalogItemVisibility.PUBLIC).getVisibility())
                .isEqualTo(CatalogItemVisibility.PUBLIC);
        assertThat(draft.complete().getStatus()).isEqualTo(CatalogItemStatus.READY);
    }
}
