package com.gcorp.service.app.mvflix_movies.catalog.domain.item;

import com.gcorp.service.app.mvflix_movies.catalog.domain.movie.EnrichmentStatus;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

import java.util.Set;

/**
 * La invariante de acceso (SHARED exige ≥1 usuario; PRIVATE/PUBLIC limpian)
 * es del dominio y se decide en {@link CatalogItem#withAccess}.
 */
class CatalogItemAccessPolicyTest {

    private static CatalogItem movie(CatalogItemVisibility visibility, Set<String> sharedWith) {
        return new CatalogItem(
                CatalogItemId.of(1L), "Javier", "Dune", CatalogItemStatus.READY,
                EnrichmentStatus.ENRICHED, null, null, visibility, sharedWith, CatalogItemKind.MOVIE);
    }

    @Test
    void sharedRequiresAtLeastOneUser() {
        assertThatThrownBy(() ->
                movie(CatalogItemVisibility.PRIVATE, Set.of())
                        .withAccess(CatalogItemVisibility.SHARED, Set.of()))
                .isInstanceOf(InvalidCatalogItemAccessException.class);
    }

    @Test
    void sharedWithNullUsersIsRejected() {
        assertThatThrownBy(() ->
                movie(CatalogItemVisibility.PRIVATE, Set.of())
                        .withAccess(CatalogItemVisibility.SHARED, null))
                .isInstanceOf(InvalidCatalogItemAccessException.class);
    }

    @Test
    void privateCleansShares() {
        var m = movie(CatalogItemVisibility.SHARED, Set.of("Maria"))
                .withAccess(CatalogItemVisibility.PRIVATE, Set.of("Maria"));

        assertThat(m.getVisibility()).isEqualTo(CatalogItemVisibility.PRIVATE);
        assertThat(m.getSharedWith()).isEmpty();
    }

    @Test
    void publicCleansShares() {
        var m = movie(CatalogItemVisibility.PRIVATE, Set.of())
                .withAccess(CatalogItemVisibility.PUBLIC, Set.of("Maria"));

        assertThat(m.getVisibility()).isEqualTo(CatalogItemVisibility.PUBLIC);
        assertThat(m.getSharedWith()).isEmpty();
    }

    @Test
    void sharedKeepsItsUsers() {
        var m = movie(CatalogItemVisibility.PRIVATE, Set.of())
                .withAccess(CatalogItemVisibility.SHARED, Set.of("Maria", "Pedro"));

        assertThat(m.getVisibility()).isEqualTo(CatalogItemVisibility.SHARED);
        assertThat(m.getSharedWith()).containsExactlyInAnyOrder("Maria", "Pedro");
    }
}
