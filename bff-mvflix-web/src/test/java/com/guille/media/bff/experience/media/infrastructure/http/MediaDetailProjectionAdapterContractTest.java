package com.guille.media.bff.experience.media.infrastructure.http;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;

import com.guille.media.bff.experience.media.application.MediaDetail;

import org.junit.jupiter.api.Test;

/**
 * Contrato al nivel del WIRE del detalle de media. Movies serializa en
 * snake_case ({@code poster_path}, {@code tmdb_id}, {@code object_id},
 * {@code enrichment_status}); sin las anotaciones, el BFF los leía como null
 * y una película vinculada aparecía como providerStatus=NONE sin poster.
 */
class MediaDetailProjectionAdapterContractTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void parsesSnakeCasePosterAndTmdbIdFromMovies() throws Exception {
        String json = """
                {"id":42,"title":"Coraline","status":"READY","kind":"MOVIE",
                 "visibility":"PRIVATE","poster_path":"/c.jpg","tmdb_id":57892,
                 "object_id":77,"enrichment_status":"ENRICHED",
                 "duration":"1h 40m","year":2009}
                """;

        var movie = this.mapper.readValue(
                json, MediaDetailProjectionAdapter.DownstreamMovie.class);

        assertThat(movie.posterPath()).isEqualTo("/c.jpg");
        assertThat(movie.tmdbId()).isEqualTo(57892L);
        assertThat(movie.objectId()).isEqualTo(77L);
        assertThat(movie.title()).isEqualTo("Coraline");
    }

    @Test
    void linkedMovieCarriesTmdbIdSoProviderStatusIsLinked() {
        var movie = new MediaDetailProjectionAdapter.DownstreamMovie(
                42L, "Coraline", null, 2009, "1h 40m",
                "/c.jpg", "texto", java.util.List.of(), null, java.util.List.of(),
                "MOVIE", "PRIVATE", "READY", "ENRICHED", 77L, 57892L);

        var detail = MediaDetail.from(new com.guille.media.bff.experience.media.application.MediaDetail.Source(
                42L, movie.title(), movie.originalTitle(), movie.year(), movie.duration(),
                movie.posterPath(), movie.overview(), movie.genres(), movie.director(),
                movie.cast(), movie.kind(), movie.visibility(), movie.status(),
                movie.objectId(), null, null, movie.tmdbId()));

        assertThat(detail.provider().status()).isEqualTo("LINKED");
        assertThat(detail.provider().providerId()).isEqualTo(57892L);
        assertThat(detail.overview().posterUrl()).isEqualTo("/c.jpg");
    }
}
