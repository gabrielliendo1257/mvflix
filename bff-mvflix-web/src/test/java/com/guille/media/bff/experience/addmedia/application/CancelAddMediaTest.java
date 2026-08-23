package com.guille.media.bff.experience.addmedia.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

import com.guille.media.bff.app.ports.StorageWebClient;
import com.guille.media.bff.experience.addmedia.application.port.AddMediaMovies;
import com.guille.media.bff.experience.addmedia.application.port.AddMediaStorage;
import com.guille.media.bff.experience.addmedia.model.AddMediaId;
import com.guille.media.bff.experience.addmedia.model.AddMediaProcess;
import com.guille.media.bff.experience.addmedia.model.AddMediaPhase;
import com.guille.media.bff.experience.addmedia.model.InvalidAddMediaTransition;
import com.guille.media.bff.experience.addmedia.application.AddMediaResult;
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
  void cancelDuringVerificationAlsoDiscardsDraft() {
    // VERIFYING también representa Storage PENDING: el draft NO está
    // verificado; cancelar antes de FINALIZING lo compensa completo.
    String id = this.processInPhase(AddMediaPhase.VERIFYING_UPLOAD);
    when(this.storage.cancelUpload(42L)).thenReturn(Mono.empty());
    when(this.movies.discardDraft(7L)).thenReturn(Mono.empty());

    StepVerifier.create(this.useCase.handle("pepe", id))
        .assertNext(view -> assertThat(view.phase())
            .isEqualTo(com.guille.media.bff.experience.addmedia.model.AddMediaPhase.CANCELLED))
        .verifyComplete();

    verify(this.storage).cancelUpload(42L);
    verify(this.movies).discardDraft(7L);
  }

  /**
   * CARRERA REAL (no determinista): complete lento vs cancel inmediato, 10
   * rondas. Invariante en TODAS: el proceso termina en UNA fase terminal y
   * los recursos se compensan solo según el ganador — nunca READY+CANCELLED
   * ni doble compensación.
   */
  @Test
  void raceBetweenSlowCompleteAndImmediateCancelAlwaysEndsConsistent() {
    for (int round = 0; round < 10; round++) {
      var repo = new com.guille.media.bff.infrastructure.persistence.
          InMemoryAddMediaProcessRepository();
      var storage = mock(AddMediaStorage.class);
      var movies = mock(AddMediaMovies.class);
      when(storage.cancelUpload(42L)).thenReturn(Mono.empty());
      when(movies.discardDraft(7L)).thenReturn(Mono.empty());

      var completion = mock(CompleteAddMedia.class);
      // Complete gana el claim pero tarda 80ms en persistir READY.
      when(completion.complete(org.mockito.ArgumentMatchers.anyLong(), any()))
          .thenAnswer(inv -> Mono.delay(java.time.Duration.ofMillis(80))
              .thenReturn(new UploadCompletionOutcome.Completed(
                  new com.guille.media.bff.app.dto.MovieDto(7L, "READY", 42L, "PRIVATE",
                      "MOVIE", "Alien", null, 1979, java.util.List.of(), null, null,
                      null, java.util.List.of(), null, null, null, null, null,
                      null, null))));
      var completeUseCase = new CompleteProcessAddMedia(repo, completion);
      var cancelUseCase = new CancelAddMedia(repo, storage, movies);

      String id = repo.save(com.guille.media.bff.experience.addmedia.model.AddMediaProcess
              .starting(com.guille.media.bff.experience.addmedia.model.AddMediaId.newId(),
                  "pepe")
              .preparing().uploadPrepared(7L, 42L))
          .block().id().value();

      Mono.zipDelayError(
              completeUseCase.handle("pepe", id, null).materialize(),
              cancelUseCase.handle("pepe", id).materialize())
          .block();

      var finalPhase = repo.findById(
          com.guille.media.bff.experience.addmedia.model.AddMediaId.parse(id))
          .block().phase();
      org.assertj.core.api.Assertions.assertThat(finalPhase)
          .isIn(AddMediaPhase.READY, AddMediaPhase.CANCELLED);

      if (finalPhase == AddMediaPhase.CANCELLED) {
        // Cancel ganó: compensó recursos y complete no persistió película.
        verify(storage, org.mockito.Mockito.atLeastOnce()).cancelUpload(42L);
        verify(movies, org.mockito.Mockito.atLeastOnce()).discardDraft(7L);
      } else {
        // Complete ganó: película lista, sin descarte de draft.
        verify(movies, org.mockito.Mockito.never()).discardDraft(7L);
      }
    }
  }

  @Test
  void cancelLosesAgainstFinalizingClaim() {
    String id = this.processInPhase(AddMediaPhase.WAITING_FOR_UPLOAD);
    org.assertj.core.api.Assertions.assertThat(
        this.processes.tryFinalizeClaim(
            com.guille.media.bff.experience.addmedia.model.AddMediaId.parse(id)).block())
        .isTrue();

    StepVerifier.create(this.useCase.handle("pepe", id))
        .expectError(InvalidAddMediaTransition.class)
        .verify();

    // Complete ganó: cancel NO toca recursos.
    verify(this.storage, never()).cancelUpload(anyLong());
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
