package com.gcorp.service.app.mvflix_movies.catalog.domain.movie;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

import java.util.List;

class MovieProviderLinkTest {

    private static final MovieMetadata RAW_METADATA = MovieMetadata.onlyTitle("Dune local");

    private static final MovieMetadata PROVIDER_METADATA = new MovieMetadata(
            "Dune", "Dune", 2021, List.of("Science Fiction"), 8.1,
            "2h 35m", "Denis Villeneuve", List.of("Timothée Chalamet"),
            "Overview", "/poster.jpg", "2021-10-22", "USA", "en",
            List.of("Oscar"), 438631L);

    @Test
    void linksProviderMetadataAndMarksMovieAsEnriched() {
        Movie movie = Movie.createDraft("Javier", RAW_METADATA, MediaKind.MOVIE);

        Movie linked = movie.linkProviderMetadata(PROVIDER_METADATA);

        assertThat(linked.getEnrichmentStatus()).isEqualTo(EnrichmentStatus.ENRICHED);
        assertThat(linked.getMetadata()).isEqualTo(PROVIDER_METADATA);
        assertThat(linked.getTitle()).isEqualTo("Dune");
    }

    @Test
    void rejectsProviderLinkForNonMovieItem() {
        Movie clip = Movie.fromLibraryAsset("Javier", RAW_METADATA, MediaKind.OTHER);

        assertThatThrownBy(() -> clip.linkProviderMetadata(PROVIDER_METADATA))
                .isInstanceOf(MovieConflictException.class);
    }

    @Test
    void rejectsProviderMetadataWithoutStableId() {
        Movie movie = Movie.createDraft("Javier", RAW_METADATA, MediaKind.MOVIE);

        assertThatThrownBy(() -> movie.linkProviderMetadata(RAW_METADATA))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void unlinksProviderAndPreservesManualMetadata() {
        Movie linked = Movie.createDraft("Javier", RAW_METADATA, MediaKind.MOVIE)
                .linkProviderMetadata(PROVIDER_METADATA);

        Movie unlinked = linked.unlinkProvider();

        assertThat(unlinked.getEnrichmentStatus()).isEqualTo(EnrichmentStatus.RAW);
        assertThat(unlinked.getMetadata().tmdbId()).isNull();
        assertThat(unlinked.getMetadata().posterPath()).isNull();
        assertThat(unlinked.getMetadata().popularity()).isNull();
        assertThat(unlinked.getMetadata().overview()).isEqualTo("Overview");
        assertThat(unlinked.getTitle()).isEqualTo("Dune");
    }
}
