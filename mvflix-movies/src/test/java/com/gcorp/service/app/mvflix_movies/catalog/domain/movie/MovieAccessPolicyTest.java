package com.gcorp.service.app.mvflix_movies.catalog.domain.movie;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

import java.util.Set;

/**
 * La invariante de acceso (SHARED exige ≥1 usuario; PRIVATE/PUBLIC limpian)
 * es del dominio y se decide en {@link Movie#withAccess}.
 */
class MovieAccessPolicyTest {

    private static Movie movie(MovieVisibility visibility, Set<String> sharedWith) {
        return new Movie(
                MovieId.of(1L), "Javier", "Dune", MovieStatus.READY,
                EnrichmentStatus.ENRICHED, null, null, visibility, sharedWith, MediaKind.MOVIE);
    }

    @Test
    void sharedRequiresAtLeastOneUser() {
        assertThatThrownBy(() ->
                movie(MovieVisibility.PRIVATE, Set.of())
                        .withAccess(MovieVisibility.SHARED, Set.of()))
                .isInstanceOf(InvalidMovieAccessException.class);
    }

    @Test
    void sharedWithNullUsersIsRejected() {
        assertThatThrownBy(() ->
                movie(MovieVisibility.PRIVATE, Set.of())
                        .withAccess(MovieVisibility.SHARED, null))
                .isInstanceOf(InvalidMovieAccessException.class);
    }

    @Test
    void privateCleansShares() {
        var m = movie(MovieVisibility.SHARED, Set.of("Maria"))
                .withAccess(MovieVisibility.PRIVATE, Set.of("Maria"));

        assertThat(m.getVisibility()).isEqualTo(MovieVisibility.PRIVATE);
        assertThat(m.getSharedWith()).isEmpty();
    }

    @Test
    void publicCleansShares() {
        var m = movie(MovieVisibility.PRIVATE, Set.of())
                .withAccess(MovieVisibility.PUBLIC, Set.of("Maria"));

        assertThat(m.getVisibility()).isEqualTo(MovieVisibility.PUBLIC);
        assertThat(m.getSharedWith()).isEmpty();
    }

    @Test
    void sharedKeepsItsUsers() {
        var m = movie(MovieVisibility.PRIVATE, Set.of())
                .withAccess(MovieVisibility.SHARED, Set.of("Maria", "Pedro"));

        assertThat(m.getVisibility()).isEqualTo(MovieVisibility.SHARED);
        assertThat(m.getSharedWith()).containsExactlyInAnyOrder("Maria", "Pedro");
    }
}
