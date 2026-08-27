package com.guille.media.bff.experience.media.application;

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
  void delegatesDeletionLifecycleWithoutReadingDetailOrApplyingSourceRules() {
    when(this.deletion.requestDeletion(42L))
        .thenReturn(Mono.just(new DeletionOutcome.Completed()));

    StepVerifier.create(this.useCase.execute(42L))
        .expectNext(new DeletionOutcome.Completed())
        .verifyComplete();

    verify(this.deletion).requestDeletion(42L);
  }

  @Test
  void propagatesPendingOutcome() {
    when(this.deletion.requestDeletion(42L))
        .thenReturn(Mono.just(new DeletionOutcome.Pending()));

    StepVerifier.create(this.useCase.execute(42L))
        .expectNext(new DeletionOutcome.Pending())
        .verifyComplete();
  }
}
