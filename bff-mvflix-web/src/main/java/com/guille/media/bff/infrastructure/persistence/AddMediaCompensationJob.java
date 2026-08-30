package com.guille.media.bff.infrastructure.persistence;

import com.guille.media.bff.experience.addmedia.application.port.AddMediaCompensationRepository;
import com.guille.media.bff.experience.addmedia.application.port.AddMediaCompensationRepository.Kind;
import com.guille.media.bff.experience.addmedia.application.port.AddMediaMovies;
import com.guille.media.bff.experience.addmedia.application.port.AddMediaStorage;
import com.guille.media.bff.experience.addmedia.application.port.AddMediaProcessRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
@Profile("!local")
@Slf4j
public class AddMediaCompensationJob {
  private final AddMediaCompensationRepository tasks;
  private final AddMediaMovies movies;
  private final AddMediaStorage storage;
  private final AddMediaProcessRepository processes;

  public AddMediaCompensationJob(AddMediaCompensationRepository tasks, AddMediaMovies movies,
      AddMediaStorage storage, AddMediaProcessRepository processes) {
    this.tasks = tasks;
    this.movies = movies;
    this.storage = storage;
    this.processes = processes;
  }

  @Scheduled(fixedDelayString = "${add-media.compensation.interval-ms:10000}")
  public void runScheduled() {
    runOnce().subscribe(null, error -> log.error("add-media compensation scheduler failed", error));
  }

  public Mono<Void> runOnce() {
    return tasks.claimPending(20)
        .flatMap(task -> execute(task)
            .then(Mono.defer(() -> tasks.markCompleted(task.id())))
            .then(Mono.defer(() -> processes.tryCompleteCancellation(task.processId()).then()))
            .onErrorResume(error -> {
              log.error("add-media compensation failed process={} kind={} resource={} attempt={}",
                  task.processId().value(), task.kind(), task.resourceId(), task.attempts(), error);
              return Mono.defer(() -> tasks.markFailed(task.id(), task.attempts(), error))
                  .onErrorResume(persistError -> {
                    log.error("add-media compensation failure could not be persisted task={}",
                        task.id(), persistError);
                    return Mono.empty();
                  });
            }), 1)
        .then();
  }

  private Mono<Void> execute(AddMediaCompensationRepository.Task task) {
    return task.kind() == Kind.DISCARD_DRAFT
        ? movies.discardDraft(task.resourceId())
        : storage.cancelUpload(task.resourceId());
  }
}
