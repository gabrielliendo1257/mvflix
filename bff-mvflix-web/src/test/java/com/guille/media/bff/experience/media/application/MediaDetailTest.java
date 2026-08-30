package com.guille.media.bff.experience.media.application;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import java.util.List;

/**
 * La derivación operacional del detalle debe coincidir con la de la grilla
 * (catalog): mismas reglas, aplicadas aquí sobre los datos del detalle.
 */
class MediaDetailTest {

  private MediaDetail.Source source(
      String domainStatus, Long objectId, Long assetId, Boolean assetPresent,
      String kind, Long tmdbId) {
    return new MediaDetail.Source(
        42L, "Coraline", null, 2009, "1h 40m", "/c.jpg", "texto", List.of(),
        null, List.of(), kind, "PRIVATE", domainStatus,
        objectId, assetId, assetPresent, tmdbId);
  }

  @Test
  void managedReadyMovieIsPlayableAndLinked() {
    var detail = MediaDetail.from(source("READY", 77L, null, null, "MOVIE", 57892L));

    assertThat(detail.access().source()).isEqualTo("MANAGED");
    assertThat(detail.media().displayStatus()).isEqualTo("READY");
    assertThat(detail.capabilities().play()).isTrue();
    assertThat(detail.provider().status()).isEqualTo("LINKED");
    assertThat(detail.provider().providerId()).isEqualTo(57892L);
    assertThat(detail.capabilities().unlinkProvider()).isTrue();
    assertThat(detail.capabilities().linkProvider()).isFalse();
  }

  @Test
  void localMissingFileDerivesMissingAndBlocksPlayDelete() {
    var detail = MediaDetail.from(source("READY", null, 17L, false, "MOVIE", null));

    assertThat(detail.access().source()).isEqualTo("LOCAL");
    assertThat(detail.media().displayStatus()).isEqualTo("MISSING");
    assertThat(detail.capabilities().play()).isFalse();
    assertThat(detail.capabilities().delete()).isFalse();
    // MOVIE sin proveedor: vincular sí.
    assertThat(detail.capabilities().linkProvider()).isTrue();
  }

  @Test
  void dualOriginSurfacesAttentionInvalid() {
    var detail = MediaDetail.from(source("READY", 77L, 17L, true, "MOVIE", null));

    assertThat(detail.access().source()).isEqualTo("INVALID");
    assertThat(detail.media().displayStatus()).isEqualTo("ATTENTION");
    assertThat(detail.capabilities().play()).isFalse();
    assertThat(detail.capabilities().delete()).isFalse();
  }

  @Test
  void otherKindNeverLinksToProviderEvenWithoutTmdb() {
    var detail = MediaDetail.from(source("READY", null, null, null, "VIDEO", null));

    assertThat(detail.provider().status()).isEqualTo("NONE");
    assertThat(detail.capabilities().linkProvider()).isFalse();
    assertThat(detail.capabilities().unlinkProvider()).isFalse();
  }

  @Test
  void draftWithNoContentIsProcessing() {
    var detail = MediaDetail.from(source("DRAFT", null, null, null, "MOVIE", null));

    assertThat(detail.media().displayStatus()).isEqualTo("PROCESSING");
    assertThat(detail.capabilities().play()).isFalse();
  }

  @Test
  void deletingMovieCannotBeDeletedAgain() {
    var detail = MediaDetail.from(source("DELETING", 77L, null, null, "MOVIE", null));

    assertThat(detail.capabilities().delete()).isFalse();
  }
}
