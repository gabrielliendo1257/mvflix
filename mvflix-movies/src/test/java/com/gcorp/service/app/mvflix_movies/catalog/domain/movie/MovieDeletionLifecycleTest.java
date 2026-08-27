package com.gcorp.service.app.mvflix_movies.catalog.domain.movie;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

import java.util.Set;

/**
 * Ciclo de vida DELETING: una vez iniciado el borrado durable, la media es un
 * estado terminal operativo — no se completa, edita, cambia de acceso ni
 * reproduce. requestDeletion es idempotente. Test puro, sin Spring.
 */
class MovieDeletionLifecycleTest {

    private static Movie movie(MovieStatus status) {
        return new Movie(
                MovieId.of(1L), "Javier", "Dune", status, EnrichmentStatus.ENRICHED,
                42L, MovieMetadata.onlyTitle("Dune"), MovieVisibility.PRIVATE,
                Set.of(), MediaKind.MOVIE);
    }

    @Test
    void requestDeletionMovesReadyToDeleting() {
        Movie deleting = movie(MovieStatus.READY).requestDeletion();

        assertThat(deleting.getStatus()).isEqualTo(MovieStatus.DELETING);
        assertThat(deleting.isDeleting()).isTrue();
    }

    @Test
    void requestDeletionIsIdempotent() {
        Movie alreadyDeleting = movie(MovieStatus.DELETING);

        Movie result = alreadyDeleting.requestDeletion();

        assertThat(result.getStatus()).isEqualTo(MovieStatus.DELETING);
    }

    @Test
    void deletingMovieCannotComplete() {
        Movie deleting = movie(MovieStatus.DELETING);

        assertThatThrownBy(() -> deleting.complete(99L))
                .isInstanceOf(MovieConflictException.class)
                .hasMessageContaining("DELETING");
    }

    @Test
    void deletingMovieCannotBeEdited() {
        Movie deleting = movie(MovieStatus.DELETING);

        assertThatThrownBy(() -> deleting.withMetadata(MovieMetadata.onlyTitle("Otro")))
                .isInstanceOf(MovieConflictException.class);
    }

    @Test
    void deletingMovieCannotChangeVisibility() {
        Movie deleting = movie(MovieStatus.DELETING);

        assertThatThrownBy(() -> deleting.withVisibility(MovieVisibility.PUBLIC))
                .isInstanceOf(MovieConflictException.class);
    }

    @Test
    void deletingMovieCannotChangeShares() {
        Movie deleting = movie(MovieStatus.DELETING);

        assertThatThrownBy(() -> deleting.withSharedWith(Set.of("Maria")))
                .isInstanceOf(MovieConflictException.class);
    }

    @Test
    void deletingMovieCannotChangeAccess() {
        Movie deleting = movie(MovieStatus.DELETING);

        assertThatThrownBy(() -> deleting.withAccess(MovieVisibility.PRIVATE, Set.of()))
                .isInstanceOf(MovieConflictException.class);
    }

    @Test
    void deletingMovieCannotLinkOrUnlinkProvider() {
        Movie deleting = movie(MovieStatus.DELETING);

        assertThatThrownBy(() -> deleting.linkProviderMetadata(
                new MovieMetadata("Dune", null, 2021, null, null, null,
                        null, null, null, null, null, null, null, null, 438631L)))
                .isInstanceOf(MovieConflictException.class);
        assertThatThrownBy(deleting::unlinkProvider)
                .isInstanceOf(MovieConflictException.class);
    }

    @Test
    void deletingMovieCannotBeReclassified() {
        Movie deleting = movie(MovieStatus.DELETING);

        assertThatThrownBy(() -> deleting.reclassifyAsOther(MovieMetadata.onlyTitle("Clip")))
                .isInstanceOf(MovieConflictException.class);
        assertThatThrownBy(deleting::reclassifyAsMovie)
                .isInstanceOf(MovieConflictException.class);
    }

    @Test
    void deletingIsNotPlayable() {
        // El playback y el catálogo derivan la reproducibilidad del status:
        // DELETING no es READY, así que no se reproduce.
        Movie deleting = movie(MovieStatus.DELETING);

        assertThat(deleting.getStatus()).isNotEqualTo(MovieStatus.READY);
        assertThat(deleting.isLibraryBacked()).isFalse();
    }

    @Test
    void nonDeletingMovieStillTransitionsNormally() {
        Movie ready = movie(MovieStatus.READY);
        Movie draft = movie(MovieStatus.DRAFT);

        assertThat(ready.withVisibility(MovieVisibility.PUBLIC).getVisibility())
                .isEqualTo(MovieVisibility.PUBLIC);
        assertThat(draft.complete(77L).getStatus()).isEqualTo(MovieStatus.READY);
    }
}
