package com.guille.media.bff.experience.media.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.guille.media.bff.experience.media.application.port.MediaDeletion;
import com.guille.media.bff.experience.media.application.port.MediaDetailProjection;

import org.junit.jupiter.api.Test;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.List;

class DeleteMediaTest {

  private final MediaDeletion deletion = mock(MediaDeletion.class);
  private final MediaDetailProjection projection = mock(MediaDetailProjection.class);
  private final DeleteMedia useCase = new DeleteMedia(this.deletion, this.projection);

  /** MediaDetail con source derivado de objectId/assetId (misma regla que el adapter). */
  private MediaDetail detail(String source) {
    MediaDetail.Source src = switch (source) {
      case "MANAGED" -> new MediaDetail.Source(42L, "Coraline", null, 2009, "1h 40m", "/c.jpg",
          null, List.of(), null, List.of(), "MOVIE", "PRIVATE", "READY", 77L, null, null, null);
      case "LOCAL" -> new MediaDetail.Source(42L, "Alien", null, 1979, "1h 57m", null,
          null, List.of(), null, List.of(), "MOVIE", "PRIVATE", "READY", null, 17L, true, null);
      case "INVALID" -> new MediaDetail.Source(42L, "Rara", null, null, null, null,
          null, List.of(), null, List.of(), "MOVIE", "PRIVATE", "READY", 77L, 17L, true, null);
      default -> new MediaDetail.Source(42L, "Beta", null, null, null, null,
          null, List.of(), null, List.of(), "MOVIE", "PRIVATE", "DRAFT", null, null, null, null);
    };
    return MediaDetail.from(src);
  }

  @Test
  void managedDeleteIsBlocked() {
    when(this.projection.detail(42L)).thenReturn(Mono.just(detail("MANAGED")));

    StepVerifier.create(this.useCase.execute(42L))
        .expectError(ManagedDeleteBlockedException.class)
        .verify();

    verify(this.deletion, never()).deleteCatalog(42L);
  }

  @Test
  void dualOriginDeleteIsBlockedToo() {
    when(this.projection.detail(42L)).thenReturn(Mono.just(detail("INVALID")));

    StepVerifier.create(this.useCase.execute(42L))
        .expectError(ManagedDeleteBlockedException.class)
        .verify();

    verify(this.deletion, never()).deleteCatalog(42L);
  }

  @Test
  void localMediaDeletesCatalogEntry() {
    when(this.projection.detail(42L)).thenReturn(Mono.just(detail("LOCAL")));
    when(this.deletion.deleteCatalog(42L)).thenReturn(Mono.just(true));

    StepVerifier.create(this.useCase.execute(42L)).verifyComplete();

    verify(this.deletion).deleteCatalog(42L);
  }

  @Test
  void draftWithoutContentDeletesCatalogEntry() {
    when(this.projection.detail(42L)).thenReturn(Mono.just(detail("NONE")));
    when(this.deletion.deleteCatalog(42L)).thenReturn(Mono.just(true));

    StepVerifier.create(this.useCase.execute(42L)).verifyComplete();

    verify(this.deletion).deleteCatalog(42L);
  }

  @Test
  void alreadyGoneIsIdempotentNoOp() {
    when(this.projection.detail(42L))
        .thenReturn(Mono.error(new MediaDetailNotFoundException(42L)));

    StepVerifier.create(this.useCase.execute(42L)).verifyComplete();

    verify(this.deletion, never()).deleteCatalog(42L);
  }

  @Test
  void downstreamFailurePropagates() {
    when(this.projection.detail(42L)).thenReturn(Mono.just(detail("LOCAL")));
    when(this.deletion.deleteCatalog(42L))
        .thenReturn(Mono.error(new IllegalStateException("downstream down")));

    StepVerifier.create(this.useCase.execute(42L))
        .expectErrorMatches(error -> {
          assertThat(error).isInstanceOf(IllegalStateException.class);
          return true;
        })
        .verify();
  }
}
