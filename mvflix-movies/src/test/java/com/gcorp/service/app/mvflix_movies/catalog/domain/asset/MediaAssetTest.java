package com.gcorp.service.app.mvflix_movies.catalog.domain.asset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.gcorp.service.app.mvflix_movies.catalog.domain.item.CatalogItemId;
import com.gcorp.service.app.mvflix_movies.library.domain.MediaAssetStatus;
import com.gcorp.service.app.mvflix_movies.library.domain.ScannedFile;
import com.gcorp.service.app.mvflix_movies.shared.domain.media.MediaAssetReference;

import java.time.Instant;

import org.junit.jupiter.api.Test;

class MediaAssetTest {

    @Test
    void managedAndLibraryAssetsShareOnlyThePlaybackContract() {
        MediaAsset managed = ManagedMediaAsset.create(CatalogItemId.of(1L), 10L, "managed/movie.mp4");
        MediaAsset library = new com.gcorp.service.app.mvflix_movies.library.domain.MediaAsset(
                com.gcorp.service.app.mvflix_movies.library.domain.MediaAssetId.of(2L),
                7L, "library/movie.mp4", 10L, "video/mp4", MediaAssetStatus.IDENTIFIED,
                com.gcorp.service.app.mvflix_movies.catalog.domain.item.CatalogItemId.of(1L), true,
                Instant.now(), Instant.now(), "admin");

        assertThat(managed.playbackReference().value()).isEqualTo("managed/movie.mp4");
        assertThat(library.playbackReference().value()).isEqualTo("library/movie.mp4");
        assertThat(managed.isPlayable()).isTrue();
        assertThat(library.isPlayable()).isTrue();
    }

    @Test
    void libraryAssetIsNotPlayableUntilIdentifiedAndPresent() {
        var asset = com.gcorp.service.app.mvflix_movies.library.domain.MediaAsset.create(
                7L, new ScannedFile("movie.mp4", 10L, "video/mp4"), "admin");

        assertThat(asset.isPlayable()).isFalse();
        assertThat(asset.identify(
                com.gcorp.service.app.mvflix_movies.catalog.domain.item.CatalogItemId.of(1L))
                .markMissing().isPlayable()).isFalse();
    }

    @Test
    void playbackReferenceRejectsBlankValues() {
        assertThatThrownBy(() -> new MediaAssetReference("  "))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
