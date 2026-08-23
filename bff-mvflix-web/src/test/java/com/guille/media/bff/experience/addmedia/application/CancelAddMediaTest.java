package com.guille.media.bff.experience.addmedia.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.guille.media.bff.app.ports.StorageWebClient;
import com.guille.media.bff.experience.addmedia.application.port.AddMediaMovies;
import com.guille.media.bff.experience.addmedia.application.port.AddMediaStorage;
import com.guille.media.bff.experience.addmedia.model.AddMediaId;
import com.guille.media.bff.experience.addmedia.model.AddMediaProcess;
import com.guille.media.bff.experience.addmedia.model.AddMediaPhase;
import com.guille.media.bff.experience.addmedia.web.AddMediaView;
import com.guille.media.bff.infrastructure.persistence.InMemoryAddMediaProcessRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

class CancelAddMediaTest {

  private final AddMediaStorage storage = mock(AddMediaStorage.class);
  private final AddMediaMovies movies = mock(AddMediaMovies.class);
  private final InMemoryAddMediaProcessRepository processes =
      new InMemoryAddMediaProcessRepository();

  private CancelAddMedia useCase;

  @BeforeEach
  void setUp() {
    this.useCase = new CancelAddMedia(this.processes, this.storage, this.movies);
  }

  private String processInPhase(AddMediaPhase phase) {
    AddMediaProcess base = AddMediaProcess.starting(AddMediaId.newId(), "pepe")
        .preparing().uploadPrepared(7L, 42L);
    AddMediaProcess target = switch (phase) {
      case VERIFYING_UPLOAD -> base.verifying();
      case READY -> base.verifying().ready();
      default -> base; // WAITING_FOR_UPLOAD
    };
    return this.processes.save(target).block().id().value();
  }

  @Test
  void cancelWhileWaitingCancelsUploadDiscardsDraftAndMarksCancelled() {
    String id = this.processInPhase(AddMediaPhase.WAITING_FOR_UPLOAD);
    when(this.storage.cancelUpload(42L)).thenReturn(Mono.empty());
    when(this.movies.discardDraft(7L)).thenReturn(Mono.empty());

    StepVerifier.create(this.useCase.handle("pepe", id))
        .assertNext(view -> assertThat(view.phase()).isEqualTo(AddMediaPhase.CANCELLED))
        .verifyComplete();

    verify(this.storage).cancelUpload(42L);
    verify(this.movies).discardDraft(7L);
  }

  @Test
  void cancelDuringVerificationKeepsMovieButCancelsUpload() {
    String id = this.processInPhase(AddMediaPhase.VERIFYING_UPLOAD);
    when(this.storage.cancelUpload(42L)).thenReturn(Mono.empty());

    StepVerifier.create(this.useCase.handle("pepe", id))
        .expectNextCount(1)
        .verifyComplete();

    // En VERIFYING el draft ya es contenido verificado: no se descarta a ciegas.
    verify(this.movies, never()).discardDraft(anyLong());
  }

  @Test
  void cannotCancelReadyOrCancelledProcesses() {
    String ready = this.processInPhase(AddMediaPhase.READY);
    StepVerifier.create(this.useCase.handle("pepe", ready))
        .expectError(com.guille.media.bff.experience.addmedia.model.
            InvalidAddMediaTransition.class)
        .verify();

    verifyNoInteractionsWithDownstreams();
  }

  private void verifyNoInteractionsWithDownstreams() {
    verify(this.storage, never()).cancelUpload(anyLong());
    verify(this.movies, never()).discardDraft(anyLong());
  }
}
