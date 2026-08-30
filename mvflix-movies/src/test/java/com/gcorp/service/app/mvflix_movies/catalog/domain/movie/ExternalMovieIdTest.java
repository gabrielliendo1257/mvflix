package com.gcorp.service.app.mvflix_movies.catalog.domain.movie;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class ExternalMovieIdTest {
    @Test
    void acceptsPositiveIds() {
        assertThat(ExternalMovieId.of(438631L).value()).isEqualTo(438631L);
    }

    @Test
    void rejectsNonPositiveIds() {
        assertThatThrownBy(() -> new ExternalMovieId(0))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void providerLinkRequiresProviderAndId() {
        assertThatThrownBy(() -> new MovieProviderLink("TMDB", null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new MovieProviderLink("", ExternalMovieId.of(1L)))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
