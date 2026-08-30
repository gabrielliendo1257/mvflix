package com.gcorp.service.app.mvflix_movies.catalog.domain.movie;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import org.junit.jupiter.api.Test;

class MovieIdentificationTest {
    @Test
    void exposesTypedLinkAndNumericTmdbProjection() {
        MovieIdentification identification = MovieIdentification.tmdb(438631L);

        assertThat(identification.providerLink()).isEqualTo(
                MovieProviderLink.tmdb(ExternalMovieId.of(438631L)));
        assertThat(identification.tmdbId()).isEqualTo(438631L);
        assertThat(identification.metadata()).isNull();
    }

    @Test
    void acceptsConfirmedMetadataAndRejectsDifferentLink() {
        MovieMetadata metadata = new MovieMetadata(
                "Dune", null, null, List.of(), null, null, null, List.of(), null,
                null, null, null, null, List.of(), MovieProviderLink.tmdb(ExternalMovieId.of(1L)));

        assertThat(MovieIdentification.of(metadata).metadata()).isEqualTo(metadata);
        assertThatThrownBy(() -> new MovieIdentification(
                MovieProviderLink.tmdb(ExternalMovieId.of(2L)), metadata))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
