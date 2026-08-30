package com.gcorp.service.app.mvflix_movies.catalog.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.gcorp.service.app.mvflix_movies.catalog.domain.movie.MovieMetadata;
import com.gcorp.service.app.mvflix_movies.catalog.domain.movie.MovieProviderLink;
import java.util.List;
import org.junit.jupiter.api.Test;

class JsonCodecMovieMetadataTest {
    @Test
    void preservesNumericTmdbIdWhileUsingTypedLinkInternally() {
        JsonCodec codec = new JsonCodec();
        MovieMetadata metadata = new MovieMetadata(
                "Dune", null, null, List.of(), null, null, null, List.of(), null,
                null, null, null, null, List.of(), MovieProviderLink.tmdb(
                        com.gcorp.service.app.mvflix_movies.catalog.domain.movie.ExternalMovieId.of(438631L)));

        String json = codec.encode(metadata);

        assertThat(json).contains("\"tmdbId\":438631").doesNotContain("providerLink");
        assertThat(codec.decode(json).providerLink()).isEqualTo(metadata.providerLink());
    }
}
