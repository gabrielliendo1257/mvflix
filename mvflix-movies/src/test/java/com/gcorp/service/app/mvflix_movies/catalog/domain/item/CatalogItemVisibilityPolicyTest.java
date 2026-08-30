package com.gcorp.service.app.mvflix_movies.catalog.domain.item;

import com.gcorp.service.app.mvflix_movies.catalog.domain.movie.EnrichmentStatus;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import java.util.Set;

/**
 * Matriz de la política de acceso del catálogo. {@link CatalogItem#isVisibleTo(String)}
 * es la fuente de verdad del dominio; {@code findVisibleCatalogItems} del repositorio
 * es su traducción SQL.
 */
class CatalogItemVisibilityPolicyTest {

    private static CatalogItem movie(CatalogItemVisibility visibility, Set<String> sharedWith) {
        return new CatalogItem(
                CatalogItemId.of(1L), "Javier", "Dune", CatalogItemStatus.READY,
                EnrichmentStatus.ENRICHED, null, null, visibility, sharedWith, CatalogItemKind.MOVIE);
    }

    @Test
    void ownerAlwaysSeesOwnMovies() {
        for (CatalogItemVisibility visibility : CatalogItemVisibility.values()) {
            CatalogItem movie = movie(visibility, Set.of("Maria"));
            assertThat(movie.isVisibleTo("Javier")).as("visibility=%s", visibility).isTrue();
        }
    }

    @Test
    void publicIsVisibleToAnyone() {
        CatalogItem movie = movie(CatalogItemVisibility.PUBLIC, Set.of());
        assertThat(movie.isVisibleTo("Pepe")).isTrue();
        assertThat(movie.isVisibleTo("Maria")).isTrue();
    }

    @Test
    void privateIsOnlyForOwner() {
        CatalogItem movie = movie(CatalogItemVisibility.PRIVATE, Set.of("Maria"));
        assertThat(movie.isVisibleTo("Maria")).isFalse();
        assertThat(movie.isVisibleTo("Pepe")).isFalse();
    }

    @Test
    void sharedIsVisibleOnlyToListedUsers() {
        CatalogItem movie = movie(CatalogItemVisibility.SHARED, Set.of("Maria"));
        assertThat(movie.isVisibleTo("Maria")).isTrue();
        assertThat(movie.isVisibleTo("Pepe")).isFalse();
    }

    @Test
    void sharedWithEmptyListIsPrivateForOthers() {
        CatalogItem movie = movie(CatalogItemVisibility.SHARED, Set.of());
        assertThat(movie.isVisibleTo("Pepe")).isFalse();
        assertThat(movie.isVisibleTo("Maria")).isFalse();
    }

    @Test
    void onlyOwnerCanManage() {
        CatalogItem movie = movie(CatalogItemVisibility.PUBLIC, Set.of());
        assertThat(movie.isOwnedBy("Javier")).isTrue();
        assertThat(movie.isOwnedBy("Maria")).isFalse();
    }

    @Test
    void withVisibilityKeepsSharedWith() {
        CatalogItem movie = movie(CatalogItemVisibility.PRIVATE, Set.of("Maria"));
        CatalogItem shared = movie.withVisibility(CatalogItemVisibility.SHARED);
        assertThat(shared.getVisibility()).isEqualTo(CatalogItemVisibility.SHARED);
        assertThat(shared.getSharedWith()).containsExactly("Maria");
        assertThat(shared.isVisibleTo("Maria")).isTrue();
    }

    @Test
    void withSharedWithChangesPolicyOutcome() {
        CatalogItem movie = movie(CatalogItemVisibility.SHARED, Set.of("Maria"));
        CatalogItem other = movie.withSharedWith(Set.of("Pedro"));
        assertThat(movie.isVisibleTo("Pedro")).isFalse();
        assertThat(other.isVisibleTo("Pedro")).isTrue();
        assertThat(other.isVisibleTo("Maria")).isFalse();
    }

    @Test
    void nullSharedWithIsTreatedAsEmpty() {
        CatalogItem movie = new CatalogItem(
                CatalogItemId.of(1L), "Javier", "Dune", CatalogItemStatus.READY,
                EnrichmentStatus.ENRICHED, null, null, CatalogItemVisibility.SHARED, null, CatalogItemKind.MOVIE);
        assertThat(movie.getSharedWith()).isEmpty();
        assertThat(movie.isVisibleTo("Pepe")).isFalse();
    }
}
