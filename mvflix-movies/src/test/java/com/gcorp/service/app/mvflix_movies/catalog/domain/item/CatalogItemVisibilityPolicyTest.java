package com.gcorp.service.app.mvflix_movies.catalog.domain.item;

import com.gcorp.service.app.mvflix_movies.catalog.domain.movie.EnrichmentStatus;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

import java.util.Set;

/**
 * Matriz de la política de acceso del catálogo. {@link CatalogItem#isVisibleTo(String)}
 * es la fuente de verdad del dominio; {@code findVisibleCatalogItems} del repositorio
 * es su traducción SQL.
 */
class CatalogItemVisibilityPolicyTest {

    private static CatalogItem movie(com.gcorp.service.app.mvflix_movies.catalog.domain.access.Visibility visibility, Set<String> sharedWith) {
        return new CatalogItem(
                CatalogItemId.of(1L), "Javier", "Dune", CatalogItemStatus.READY,
                EnrichmentStatus.ENRICHED, null, null, visibility, sharedWith, CatalogItemKind.MOVIE);
    }

    @Test
    void ownerAlwaysSeesOwnMovies() {
        for (com.gcorp.service.app.mvflix_movies.catalog.domain.access.Visibility visibility : com.gcorp.service.app.mvflix_movies.catalog.domain.access.Visibility.values()) {
            CatalogItem movie = movie(visibility, Set.of("Maria"));
            assertThat(movie.isVisibleTo("Javier")).as("visibility=%s", visibility).isTrue();
        }
    }

    @Test
    void publicIsVisibleToAnyone() {
        CatalogItem movie = movie(com.gcorp.service.app.mvflix_movies.catalog.domain.access.Visibility.PUBLIC, Set.of());
        assertThat(movie.isVisibleTo("Pepe")).isTrue();
        assertThat(movie.isVisibleTo("Maria")).isTrue();
    }

    @Test
    void privateIsOnlyForOwner() {
        CatalogItem movie = movie(com.gcorp.service.app.mvflix_movies.catalog.domain.access.Visibility.PRIVATE, Set.of("Maria"));
        assertThat(movie.isVisibleTo("Maria")).isFalse();
        assertThat(movie.isVisibleTo("Pepe")).isFalse();
    }

    @Test
    void sharedIsVisibleOnlyToListedUsers() {
        CatalogItem movie = movie(com.gcorp.service.app.mvflix_movies.catalog.domain.access.Visibility.SHARED, Set.of("Maria"));
        assertThat(movie.isVisibleTo("Maria")).isTrue();
        assertThat(movie.isVisibleTo("Pepe")).isFalse();
    }

    @Test
    void sharedWithEmptyListIsPrivateForOthers() {
        assertThatThrownBy(() -> movie(com.gcorp.service.app.mvflix_movies.catalog.domain.access.Visibility.SHARED, Set.of()))
                .isInstanceOf(InvalidCatalogItemAccessException.class);
    }

    @Test
    void onlyOwnerCanManage() {
        CatalogItem movie = movie(com.gcorp.service.app.mvflix_movies.catalog.domain.access.Visibility.PUBLIC, Set.of());
        assertThat(movie.isOwnedBy("Javier")).isTrue();
        assertThat(movie.isOwnedBy("Maria")).isFalse();
    }

    @Test
    void withVisibilityKeepsSharedWith() {
        CatalogItem movie = movie(com.gcorp.service.app.mvflix_movies.catalog.domain.access.Visibility.SHARED, Set.of("Maria"));
        CatalogItem shared = movie.withVisibility(com.gcorp.service.app.mvflix_movies.catalog.domain.access.Visibility.SHARED);
        assertThat(shared.getVisibility()).isEqualTo(com.gcorp.service.app.mvflix_movies.catalog.domain.access.Visibility.SHARED);
        assertThat(shared.getSharedWith()).containsExactly("Maria");
        assertThat(shared.isVisibleTo("Maria")).isTrue();
    }

    @Test
    void withSharedWithChangesPolicyOutcome() {
        CatalogItem movie = movie(com.gcorp.service.app.mvflix_movies.catalog.domain.access.Visibility.SHARED, Set.of("Maria"));
        CatalogItem other = movie.withSharedWith(Set.of("Pedro"));
        assertThat(movie.isVisibleTo("Pedro")).isFalse();
        assertThat(other.isVisibleTo("Pedro")).isTrue();
        assertThat(other.isVisibleTo("Maria")).isFalse();
    }

    @Test
    void nullSharedWithIsRejectedForSharedVisibility() {
        assertThatThrownBy(() -> new CatalogItem(
                CatalogItemId.of(1L), "Javier", "Dune", CatalogItemStatus.READY,
                EnrichmentStatus.ENRICHED, null, null, com.gcorp.service.app.mvflix_movies.catalog.domain.access.Visibility.SHARED, null, CatalogItemKind.MOVIE))
                .isInstanceOf(InvalidCatalogItemAccessException.class);
    }
}
