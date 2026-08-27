package com.guille.media.bff.experience.media.infrastructure.http;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.guille.media.bff.app.ports.MoviesWebClient;
import com.guille.media.bff.experience.media.application.DeletionOutcome;

import org.junit.jupiter.api.Test;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

class MediaDeletionAdapterTest {

  private final MoviesWebClient movies = mock(MoviesWebClient.class);
  private final MediaDeletionAdapter adapter = new MediaDeletionAdapter(this.movies);

  @Test
  void delegatesToMoviesAndPropagatesCompleted() {
    when(this.movies.requestDeletion(42L))
        .thenReturn(Mono.just(new DeletionOutcome.Completed()));

    StepVerifier.create(this.adapter.requestDeletion(42L))
        .expectNext(new DeletionOutcome.Completed())
        .verifyComplete();

    verify(this.movies).requestDeletion(42L);
  }

  @Test
  void propagatesPendingWithoutCallingStorage() {
    when(this.movies.requestDeletion(42L))
        .thenReturn(Mono.just(new DeletionOutcome.Pending()));

    StepVerifier.create(this.adapter.requestDeletion(42L))
        .expectNext(new DeletionOutcome.Pending())
        .verifyComplete();
  }
}
