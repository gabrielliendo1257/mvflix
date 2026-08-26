package com.guille.media.bff.experience.media.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.guille.media.bff.experience.media.application.port.MediaDeletion;

import org.junit.jupiter.api.Test;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

class DeleteMediaTest {

  private final MediaDeletion deletion = mock(MediaDeletion.class);
  private final DeleteMedia useCase = new DeleteMedia(this.deletion);

  @Test
  void deletesCatalogEntryAndCompletes() {
    when(this.deletion.deleteCatalog(42L)).thenReturn(Mono.just(true));

    StepVerifier.create(this.useCase.execute(42L)).verifyComplete();

    verify(this.deletion).deleteCatalog(42L);
  }

  @Test
  void alreadyAbsentIsStillSuccessIdempotent() {
    when(this.deletion.deleteCatalog(42L)).thenReturn(Mono.just(false));

    StepVerifier.create(this.useCase.execute(42L)).verifyComplete();

    verify(this.deletion).deleteCatalog(42L);
  }

  @Test
  void downstreamFailurePropagates() {
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
