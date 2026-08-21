package com.gcorp.service.app.mvflix_movies.domain.movie;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import java.util.Set;

/**
 * Matriz de la política de acceso del catálogo. {@link Movie#isVisibleTo(String)}
 * es la fuente de verdad del dominio; {@code findVisibleMovies} del repositorio
 * es su traducción SQL.
 */
class MovieVisibilityPolicyTest {

    private static Movie movie(MovieVisibility visibility, Set<String> sharedWith) {
        return new Movie(
                MovieId.of(1L), "Javier", "Dune", MovieStatus.READY,
                EnrichmentStatus.ENRICHED, null, null, visibility, sharedWith, MediaKind.MOVIE);
    }

    @Test
    void ownerAlwaysSeesOwnMovies() {
        for (MovieVisibility visibility : MovieVisibility.values()) {
            Movie movie = movie(visibility, Set.of("Maria"));
            assertThat(movie.isVisibleTo("Javier")).as("visibility=%s", visibility).isTrue();
        }
    }

    @Test
    void publicIsVisibleToAnyone() {
        Movie movie = movie(MovieVisibility.PUBLIC, Set.of());
        assertThat(movie.isVisibleTo("Pepe")).isTrue();
        assertThat(movie.isVisibleTo("Maria")).isTrue();
    }

    @Test
    void privateIsOnlyForOwner() {
        Movie movie = movie(MovieVisibility.PRIVATE, Set.of("Maria"));
        assertThat(movie.isVisibleTo("Maria")).isFalse();
        assertThat(movie.isVisibleTo("Pepe")).isFalse();
    }

    @Test
    void sharedIsVisibleOnlyToListedUsers() {
        Movie movie = movie(MovieVisibility.SHARED, Set.of("Maria"));
        assertThat(movie.isVisibleTo("Maria")).isTrue();
        assertThat(movie.isVisibleTo("Pepe")).isFalse();
    }

    @Test
    void sharedWithEmptyListIsPrivateForOthers() {
        Movie movie = movie(MovieVisibility.SHARED, Set.of());
        assertThat(movie.isVisibleTo("Pepe")).isFalse();
        assertThat(movie.isVisibleTo("Maria")).isFalse();
    }

    @Test
    void onlyOwnerCanManage() {
        Movie movie = movie(MovieVisibility.PUBLIC, Set.of());
        assertThat(movie.isOwnedBy("Javier")).isTrue();
        assertThat(movie.isOwnedBy("Maria")).isFalse();
    }

    @Test
    void withVisibilityKeepsSharedWith() {
        Movie movie = movie(MovieVisibility.PRIVATE, Set.of("Maria"));
        Movie shared = movie.withVisibility(MovieVisibility.SHARED);
        assertThat(shared.getVisibility()).isEqualTo(MovieVisibility.SHARED);
        assertThat(shared.getSharedWith()).containsExactly("Maria");
        assertThat(shared.isVisibleTo("Maria")).isTrue();
    }

    @Test
    void withSharedWithChangesPolicyOutcome() {
        Movie movie = movie(MovieVisibility.SHARED, Set.of("Maria"));
        Movie other = movie.withSharedWith(Set.of("Pedro"));
        assertThat(movie.isVisibleTo("Pedro")).isFalse();
        assertThat(other.isVisibleTo("Pedro")).isTrue();
        assertThat(other.isVisibleTo("Maria")).isFalse();
    }

    @Test
    void nullSharedWithIsTreatedAsEmpty() {
        Movie movie = new Movie(
                MovieId.of(1L), "Javier", "Dune", MovieStatus.READY,
                EnrichmentStatus.ENRICHED, null, null, MovieVisibility.SHARED, null, MediaKind.MOVIE);
        assertThat(movie.getSharedWith()).isEmpty();
        assertThat(movie.isVisibleTo("Pepe")).isFalse();
    }
}