package com.guille.media.reproductor.uploader.storage.domain.vos;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class StorageFolderTest {

  @Test
  void mapsVideoMimeToVideos() {
    assertThat(StorageFolder.from(MimeType.of("video/mp4"))).isEqualTo(StorageFolder.VIDEOS);
    assertThat(StorageFolder.from(MimeType.of("video/webm"))).isEqualTo(StorageFolder.VIDEOS);
  }

  @Test
  void mapsImageMimeToImages() {
    assertThat(StorageFolder.from(MimeType.of("image/png"))).isEqualTo(StorageFolder.IMAGES);
    assertThat(StorageFolder.from(MimeType.of("image/jpeg"))).isEqualTo(StorageFolder.IMAGES);
  }

  @Test
  void mapsArchivesToCompressed() {
    assertThat(StorageFolder.from(MimeType.of("application/zip"))).isEqualTo(StorageFolder.COMPRESSED);
    assertThat(StorageFolder.from(MimeType.of("application/gzip"))).isEqualTo(StorageFolder.COMPRESSED);
  }

  @Test
  void mapsExecutablesAndBinariesToExecutables() {
    assertThat(StorageFolder.from(MimeType.of("application/octet-stream")))
        .isEqualTo(StorageFolder.EXECUTABLES);
    assertThat(StorageFolder.from(MimeType.of("application/x-executable")))
        .isEqualTo(StorageFolder.EXECUTABLES);
  }

  @Test
  void unknownMimeFallsBackToPrivate() {
    assertThat(StorageFolder.from(MimeType.of("text/plain"))).isEqualTo(StorageFolder.PRIVATE);
    assertThat(StorageFolder.from(MimeType.of("application/pdf"))).isEqualTo(StorageFolder.PRIVATE);
  }
}