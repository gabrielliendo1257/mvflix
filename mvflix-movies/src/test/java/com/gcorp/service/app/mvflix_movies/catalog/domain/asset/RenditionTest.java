package com.gcorp.service.app.mvflix_movies.catalog.domain.asset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class RenditionTest {

    @Test
    void requestedRenditionCanBecomeReadyOrFailed() {
        var requested = Rendition.requested(MediaAssetId.of(1L), RenditionOrigin.MEDIA_ASSET, "1080p");

        assertThat(requested.getStatus()).isEqualTo(RenditionStatus.REQUESTED);
        assertThat(requested.ready(StorageObjectId.of(2L), new RenditionTechnicalMetadata(
                "movie.mp4", 120L, "mp4", "h264", "1920x1080")).getStatus())
                .isEqualTo(RenditionStatus.READY);
        assertThat(requested.failed().getStatus()).isEqualTo(RenditionStatus.FAILED);
    }

    @Test
    void readyRenditionRequiresStorageAndValidTechnicalMetadata() {
        assertThatThrownBy(() -> new Rendition(null, MediaAssetId.of(1L), RenditionOrigin.MEDIA,
                null, "1080p", RenditionStatus.READY, null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(new Rendition(null, MediaAssetId.of(1L), RenditionOrigin.MEDIA,
                StorageObjectId.of(1L), "1080p", RenditionStatus.READY, null).getStatus())
                .isEqualTo(RenditionStatus.READY);
        assertThatThrownBy(() -> new RenditionTechnicalMetadata(null, -1L, null, null, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void profileAndSourceAreRequired() {
        assertThatThrownBy(() -> Rendition.requested(MediaAssetId.of(1L), RenditionOrigin.MEDIA, " "))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Rendition(null, MediaAssetId.of(1L), null, null,
                "1080p", RenditionStatus.REQUESTED, null))
                .isInstanceOf(NullPointerException.class);
    }
}
