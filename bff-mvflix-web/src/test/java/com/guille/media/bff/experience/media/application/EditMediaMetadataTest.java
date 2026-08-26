package com.guille.media.bff.experience.media.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;

import com.guille.media.bff.experience.media.application.port.MetadataActions;
import com.guille.media.bff.experience.media.application.port.MediaDetailProjection;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.List;

class EditMediaMetadataTest {

  private final MetadataActions actions = mock(MetadataActions.class);
  private final MediaDetailProjection projection = mock(MediaDetailProjection.class);
  private final EditMediaMetadata useCase =
      new EditMediaMetadata(this.actions, this.projection);

  @Test
  void appliesPatchThenReturnsRefreshedDetail() {
    MetadataPatch patch = new MetadataPatch(
        "Título nuevo", null, 2024, List.of(), null, null, List.of(),
        null, null, null, null, null, null, null, null);
    when(this.actions.updateMetadata(
            org.mockito.ArgumentMatchers.eq(42L),
            org.mockito.ArgumentMatchers.any())).thenReturn(Mono.empty());
    when(this.projection.detail(org.mockito.ArgumentMatchers.anyLong()))
        .thenReturn(Mono.just(MediaDetail.from(new MediaDetail.Source(
            42L, "Título nuevo", null, 2024, null, null, null,
            java.util.List.of(), null, java.util.List.of(),
            "MOVIE", "PRIVATE", "READY", 77L, null, null, null))));

    StepVerifier.create(this.useCase.execute(42L, patch))
        .assertNext(detail -> {
          assertThat(detail.overview().title()).isEqualTo("Título nuevo");
          assertThat(detail.overview().year()).isEqualTo(2024);
          assertThat(detail.media().mediaId()).isEqualTo(42L);
        })
        .verifyComplete();

    verify(this.actions).updateMetadata(42L, patch);
    verify(this.projection).detail(42L);
  }

  @Test
  void mutationFailurePropagatesWithoutRefreshing() {
    MetadataPatch patch = new MetadataPatch(
        null, null, null, null, null, null, null, null,
        null, null, null, null, null, null, null);
    when(this.actions.updateMetadata(
            org.mockito.ArgumentMatchers.eq(42L),
            org.mockito.ArgumentMatchers.any()))
        .thenReturn(Mono.error(new IllegalStateException("downstream")));

    StepVerifier.create(this.useCase.execute(42L, patch))
        .expectError(IllegalStateException.class)
        .verify();

    org.mockito.Mockito.verifyNoInteractions(this.projection);
  }
}
