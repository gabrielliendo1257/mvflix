package com.gcorp.service.app.mvflix_movies.library.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.gcorp.service.app.mvflix_movies.catalog.domain.item.CatalogItemId;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class MediaAssetTechnicalMetadataTest {

    private static final CatalogItemId MOVIE = CatalogItemId.of(42L);

    @Test
    void lifecycleTransitionsPreserveTechnicalMetadata() {
        MediaAsset asset = identifiedAsset();
        MediaAsset unidentified = new MediaAsset(MediaAssetId.of(7L), 3L, "movie.mkv", 10L,
                "video/x-matroska", MediaAssetStatus.UNIDENTIFIED, null, true,
                asset.getCreatedAt(), asset.getUpdatedAt(), "tester", "movie.mkv", 120L,
                "matroska", "h264", "1920x1080", "library/movie.mkv");

        assertTechnicalMetadata(unidentified.identify(MOVIE));
        assertTechnicalMetadata(asset.unidentify());
        assertTechnicalMetadata(asset.markMissing());
        assertTechnicalMetadata(asset.markPresent());
        assertTechnicalMetadata(asset.refresh(99L, "video/x-matroska"));
    }

    @Test
    void technicalMetadataCanBeReplacedWithoutChangingLifecycleOrTimestamps() {
        MediaAsset asset = identifiedAsset();

        MediaAsset updated = asset.withTechnicalMetadata(
                "new-name.mp4", 321L, "mp4", "h265", "3840x2160", "library/new-name.mp4");

        assertThat(updated.getId()).isEqualTo(asset.getId());
        assertThat(updated.getCatalogItemId()).isEqualTo(MOVIE);
        assertThat(updated.getStatus()).isEqualTo(MediaAssetStatus.IDENTIFIED);
        assertThat(updated.isPresent()).isTrue();
        assertThat(updated.getCreatedAt()).isEqualTo(asset.getCreatedAt());
        assertThat(updated.getUpdatedAt()).isEqualTo(asset.getUpdatedAt());
        assertThat(updated.getFilename()).isEqualTo("new-name.mp4");
        assertThat(updated.getDuration()).isEqualTo(321L);
        assertThat(updated.getContainer()).isEqualTo("mp4");
        assertThat(updated.getVideoCodec()).isEqualTo("h265");
        assertThat(updated.getResolution()).isEqualTo("3840x2160");
        assertThat(updated.getStorageReference()).isEqualTo("library/new-name.mp4");
    }

    private static MediaAsset identifiedAsset() {
        Instant created = Instant.parse("2026-01-01T00:00:00Z");
        Instant updated = Instant.parse("2026-01-02T00:00:00Z");
        return new MediaAsset(MediaAssetId.of(7L), 3L, "movie.mkv", 10L,
                "video/x-matroska", MediaAssetStatus.IDENTIFIED, MOVIE, true,
                created, updated, "tester", "movie.mkv", 120L, "matroska",
                "h264", "1920x1080", "library/movie.mkv");
    }

    private static void assertTechnicalMetadata(MediaAsset asset) {
        assertThat(asset.getFilename()).isEqualTo("movie.mkv");
        assertThat(asset.getDuration()).isEqualTo(120L);
        assertThat(asset.getContainer()).isEqualTo("matroska");
        assertThat(asset.getVideoCodec()).isEqualTo("h264");
        assertThat(asset.getResolution()).isEqualTo("1920x1080");
        assertThat(asset.getStorageReference()).isEqualTo("library/movie.mkv");
    }
}
