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

    private static CatalogItem movie(com.gcorp.service.app.mvflix_movies.catalog.domain.access.Visibility visibility, Set<String> sharedWith) {
        return new CatalogItem(
                CatalogItemId.of(1L), "Javier", "Dune", CatalogItemStatus.READY,
                EnrichmentStatus.ENRICHED, null, null, visibility, sharedWith, CatalogItemKind.MOVIE);
    }

    @Test
    void sharedRequiresAtLeastOneUser() {
        assertThatThrownBy(() ->
                movie(com.gcorp.service.app.mvflix_movies.catalog.domain.access.Visibility.PRIVATE, Set.of())
                        .withAccess(com.gcorp.service.app.mvflix_movies.catalog.domain.access.Visibility.SHARED, Set.of()))
                .isInstanceOf(InvalidCatalogItemAccessException.class);
    }

    @Test
    void sharedWithNullUsersIsRejected() {
        assertThatThrownBy(() ->
                movie(com.gcorp.service.app.mvflix_movies.catalog.domain.access.Visibility.PRIVATE, Set.of())
                        .withAccess(com.gcorp.service.app.mvflix_movies.catalog.domain.access.Visibility.SHARED, null))
                .isInstanceOf(InvalidCatalogItemAccessException.class);
    }

    @Test
    void privateCleansShares() {
        var m = movie(com.gcorp.service.app.mvflix_movies.catalog.domain.access.Visibility.SHARED, Set.of("Maria"))
                .withAccess(com.gcorp.service.app.mvflix_movies.catalog.domain.access.Visibility.PRIVATE, Set.of("Maria"));

        assertThat(m.getVisibility()).isEqualTo(com.gcorp.service.app.mvflix_movies.catalog.domain.access.Visibility.PRIVATE);
        assertThat(m.getSharedWith()).isEmpty();
    }

    @Test
    void publicCleansShares() {
        var m = movie(com.gcorp.service.app.mvflix_movies.catalog.domain.access.Visibility.PRIVATE, Set.of())
                .withAccess(com.gcorp.service.app.mvflix_movies.catalog.domain.access.Visibility.PUBLIC, Set.of("Maria"));

        assertThat(m.getVisibility()).isEqualTo(com.gcorp.service.app.mvflix_movies.catalog.domain.access.Visibility.PUBLIC);
        assertThat(m.getSharedWith()).isEmpty();
    }

    @Test
    void sharedKeepsItsUsers() {
        var m = movie(com.gcorp.service.app.mvflix_movies.catalog.domain.access.Visibility.PRIVATE, Set.of())
                .withAccess(com.gcorp.service.app.mvflix_movies.catalog.domain.access.Visibility.SHARED, Set.of("Maria", "Pedro"));

        assertThat(m.getVisibility()).isEqualTo(com.gcorp.service.app.mvflix_movies.catalog.domain.access.Visibility.SHARED);
        assertThat(m.getSharedWith()).containsExactlyInAnyOrder("Maria", "Pedro");
    }
}
