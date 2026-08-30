package com.guille.media.bff.infrastructure.persistence;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.guille.media.bff.experience.addmedia.application.port.AddMediaCompensationRepository;
import com.guille.media.bff.experience.addmedia.application.port.AddMediaCompensationRepository.Kind;
import com.guille.media.bff.experience.addmedia.application.port.AddMediaMovies;
import com.guille.media.bff.experience.addmedia.application.port.AddMediaStorage;
import com.guille.media.bff.experience.addmedia.model.AddMediaId;
import com.guille.media.bff.experience.addmedia.application.port.AddMediaProcessRepository;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import static org.assertj.core.api.Assertions.assertThat;

class AddMediaCompensationJobTest {
  private final AddMediaCompensationRepository tasks = org.mockito.Mockito.mock(AddMediaCompensationRepository.class);
  private final AddMediaMovies movies = org.mockito.Mockito.mock(AddMediaMovies.class);
  private final AddMediaStorage storage = org.mockito.Mockito.mock(AddMediaStorage.class);
  private final AddMediaProcessRepository processes = org.mockito.Mockito.mock(AddMediaProcessRepository.class);
  private final AddMediaId process = AddMediaId.newId();

  @Test
  void successfulTaskIsCompleted() {
    var task = new AddMediaCompensationRepository.Task(1, process, Kind.DISCARD_DRAFT, 7L, 1, null);
    when(tasks.claimPending(anyInt())).thenReturn(Flux.just(task));
    when(movies.discardDraft(7L)).thenReturn(Mono.empty());
    when(tasks.markCompleted(1)).thenReturn(Mono.empty());

    when(processes.tryCompleteCancellation(process)).thenReturn(Mono.just(true));
    StepVerifier.create(new AddMediaCompensationJob(tasks, movies, storage, processes).runOnce())
        .verifyComplete();

    verify(movies).discardDraft(7L);
    verify(tasks).markCompleted(1);
    verify(processes).tryCompleteCancellation(process);
  }

  @Test
  void failedTaskIsPersistedForRetry() {
    var task = new AddMediaCompensationRepository.Task(2, process, Kind.CANCEL_UPLOAD, 42L, 3, "old");
    RuntimeException failure = new RuntimeException("storage down");
    when(tasks.claimPending(anyInt())).thenReturn(Flux.just(task));
    when(storage.cancelUpload(42L)).thenReturn(Mono.error(failure));
    when(tasks.markFailed(2, 3, failure)).thenReturn(Mono.empty());

    StepVerifier.create(new AddMediaCompensationJob(tasks, movies, storage, processes).runOnce())
        .verifyComplete();

    verify(tasks).markFailed(2, 3, failure);
  }

  @Test
  void multipleTasksCompleteCancellationOnlyAfterTheLastOne() {
    var repository = new InMemoryAddMediaProcessRepository();
    var id = AddMediaId.newId();
    var cancelling = com.guille.media.bff.experience.addmedia.model.AddMediaProcess
        .starting(id, "pepe").preparing().uploadPrepared(7L, 42L);
    repository.save(cancelling).block();
    repository.tryCancelClaim(id).block();
    repository.enqueue(id, Kind.DISCARD_DRAFT, 7L, new RuntimeException("one")).block();
    repository.enqueue(id, Kind.CANCEL_UPLOAD, 42L, new RuntimeException("two")).block();

    var first = repository.claimPending(1).blockFirst();
    repository.markCompleted(first.id()).block();
    assertThat(repository.tryCompleteCancellation(id).block()).isFalse();

    when(movies.discardDraft(7L)).thenReturn(Mono.empty());
    when(storage.cancelUpload(42L)).thenReturn(Mono.empty());
    var job = new AddMediaCompensationJob(repository, movies, storage, repository);

    job.runOnce().block();

    assertThat(repository.findById(id).block().phase()).isEqualTo(
        com.guille.media.bff.experience.addmedia.model.AddMediaPhase.CANCELLED);
  }

  @Test
  void preparingWithoutResourcesIsReturnedToStarting() {
    var repository = new InMemoryAddMediaProcessRepository();
    var id = AddMediaId.newId();
    repository.save(com.guille.media.bff.experience.addmedia.model.AddMediaProcess
        .starting(id, "pepe").preparing()).block();

    assertThat(repository.completePreparingRecovery(id).block()).isTrue();
    assertThat(repository.findById(id).block().phase()).isEqualTo(
        com.guille.media.bff.experience.addmedia.model.AddMediaPhase.STARTING);
  }

  @Test
  void preparingWithMovieAndUnknownUploadIsNotCancelledByRepositoryAlone() {
    var repository = new InMemoryAddMediaProcessRepository();
    var id = AddMediaId.newId();
    repository.save(com.guille.media.bff.experience.addmedia.model.AddMediaProcess
        .starting(id, "pepe").preparing().withMovieId(7L)).block();

    assertThat(repository.completePreparingRecovery(id).block()).isFalse();
    assertThat(repository.findById(id).block().phase()).isEqualTo(
        com.guille.media.bff.experience.addmedia.model.AddMediaPhase.PREPARING);
  }

  @Test
  void recoveredUploadCanBeClaimedAndIsNotLost() {
    var repository = new InMemoryAddMediaProcessRepository();
    var id = AddMediaId.newId();
    var preparing = com.guille.media.bff.experience.addmedia.model.AddMediaProcess
        .starting(id, "pepe").preparing().withMovieId(7L);
    repository.save(preparing).block();

    assertThat(repository.claimRecoveredCancellation(id, preparing.version(), 42L).block())
        .isTrue();
    assertThat(repository.findById(id).block().uploadId()).isEqualTo(42L);
    assertThat(repository.findById(id).block().phase()).isEqualTo(
        com.guille.media.bff.experience.addmedia.model.AddMediaPhase.CANCELLING);
  }
}
