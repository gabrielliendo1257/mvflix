package com.gcorp.service.app.mvflix_movies.catalog.domain.movie;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.time.Instant;

class MovieProviderLinkTest {

    private static final MovieMetadata RAW_METADATA = MovieMetadata.onlyTitle("Dune local");

    private static final MovieMetadata PROVIDER_METADATA = new MovieMetadata(
            "Dune", "Dune", 2021, List.of("Science Fiction"), 8.1,
            "2h 35m", "Denis Villeneuve", List.of("Timothée Chalamet"),
            "Overview", "/poster.jpg", "2021-10-22", "USA", "en",
            List.of("Oscar"), 438631L);

    @Test
    void linksProviderMetadataAndMarksMovieAsEnriched() {
        CatalogItem movie = CatalogItem.createDraft("Javier", RAW_METADATA, MediaKind.MOVIE);

        CatalogItem linked = movie.linkProviderMetadata(PROVIDER_METADATA);

        assertThat(linked.getEnrichmentStatus()).isEqualTo(EnrichmentStatus.ENRICHED);
        assertThat(linked.getMetadata()).isEqualTo(PROVIDER_METADATA);
        assertThat(linked.getTitle()).isEqualTo("Dune");
    }

    @Test
    void rejectsProviderLinkForNonMovieItem() {
        CatalogItem clip = CatalogItem.fromLibraryAsset(
                "Javier", new VideoMetadata("Family clip", "Home video", Instant.parse("2024-01-01T00:00:00Z")), MediaKind.VIDEO);

        assertThatThrownBy(() -> clip.linkProviderMetadata(PROVIDER_METADATA))
                .isInstanceOf(CatalogItemConflictException.class);
    }

    @Test
    void videoMetadataCannotBeStoredAsMovie() {
        assertThatThrownBy(() -> CatalogItem.createDraft(
                "Javier", new VideoMetadata("Clip", "Description", null), MediaKind.MOVIE))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("metadata does not match catalog kind");
    }

    @Test
    void videoCannotUnlinkProvider() {
        CatalogItem clip = CatalogItem.fromLibraryAsset(
                "Javier", new VideoMetadata("Family clip", "Home video", null), MediaKind.VIDEO);

        assertThatThrownBy(clip::unlinkProvider)
                .isInstanceOf(CatalogItemConflictException.class);
    }

    @Test
    void rejectsProviderMetadataWithoutStableId() {
        CatalogItem movie = CatalogItem.createDraft("Javier", RAW_METADATA, MediaKind.MOVIE);

        assertThatThrownBy(() -> movie.linkProviderMetadata(RAW_METADATA))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void unlinksProviderAndPreservesManualMetadata() {
        CatalogItem linked = CatalogItem.createDraft("Javier", RAW_METADATA, MediaKind.MOVIE)
                .linkProviderMetadata(PROVIDER_METADATA);

        CatalogItem unlinked = linked.unlinkProvider();

        assertThat(unlinked.getEnrichmentStatus()).isEqualTo(EnrichmentStatus.RAW);
        assertThat(unlinked.getMovieMetadata().tmdbId()).isNull();
        assertThat(unlinked.getMovieMetadata().posterPath()).isNull();
        assertThat(unlinked.getMovieMetadata().popularity()).isNull();
        assertThat(unlinked.getMovieMetadata().overview()).isEqualTo("Overview");
        assertThat(unlinked.getTitle()).isEqualTo("Dune");
    }

    @Test
    void reclassifiesMovieAsOtherAndDropsProviderIdentity() {
        CatalogItem linked = CatalogItem.createDraft("Javier", RAW_METADATA, MediaKind.MOVIE)
                .linkProviderMetadata(PROVIDER_METADATA);
        MovieMetadata manualMetadata = new MovieMetadata(
                "Grabación familiar", null, null, List.of(), null, null, null,
                List.of(), "Metadata manual", "/provider-poster.jpg", null, null,
                null, List.of(), 438631L);

        CatalogItem reclassified = linked.reclassifyAsVideo(manualMetadata);

        assertThat(reclassified.getKind()).isEqualTo(MediaKind.VIDEO);
        assertThat(reclassified.getEnrichmentStatus()).isEqualTo(EnrichmentStatus.RAW);
        assertThat(reclassified.getTitle()).isEqualTo("Grabación familiar");
        assertThat(reclassified.getMetadata()).isEqualTo(new VideoMetadata("Grabación familiar", "Metadata manual", null));
    }

    @Test
    void rejectsReclassificationWithoutTitle() {
        CatalogItem movie = CatalogItem.createDraft("Javier", RAW_METADATA, MediaKind.MOVIE);
        MovieMetadata missingTitle = new MovieMetadata(
                null, null, null, List.of(), null, null, null, List.of(), null,
                null, null, null, null, List.of(), null);

        assertThatThrownBy(() -> movie.reclassifyAsVideo(missingTitle))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("movie title is required");
    }

    @Test
    void reclassifiesOtherAsRawMovieWithoutProvider() {
        MovieMetadata inconsistentProviderMetadata = new MovieMetadata(
                "Imported clip", null, null, List.of(), 8.0, null, null,
                List.of(), null, "/poster.jpg", null, null, null, List.of(), 99L);
        CatalogItem clip = CatalogItem.fromLibraryAsset(
                "Javier", inconsistentProviderMetadata, MediaKind.VIDEO);

        CatalogItem reclassified = clip.reclassifyAsMovie();

        assertThat(reclassified.getKind()).isEqualTo(MediaKind.MOVIE);
        assertThat(reclassified.getEnrichmentStatus()).isEqualTo(EnrichmentStatus.RAW);
        assertThat(reclassified.getMovieMetadata().tmdbId()).isNull();
        assertThat(reclassified.getMovieMetadata().posterPath()).isNull();
        assertThat(reclassified.getMovieMetadata().popularity()).isNull();
    }
}
