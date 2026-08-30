package com.guille.media.bff.infrastructure.persistence;

import com.guille.media.bff.experience.addmedia.application.port.AddMediaProcessRepository;
import com.guille.media.bff.experience.addmedia.model.AddMediaId;
import com.guille.media.bff.experience.addmedia.model.AddMediaProcess;
import com.guille.media.bff.experience.addmedia.model.ConcurrentProcessUpdateException;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

import org.springframework.stereotype.Component;

import reactor.core.publisher.Mono;
import reactor.core.publisher.Flux;
import com.guille.media.bff.experience.addmedia.application.port.AddMediaCompensationRepository;
import com.guille.media.bff.experience.addmedia.application.port.AddMediaCompensationRepository.Kind;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Adaptador EN MEMORIA del estado de proceso Add Media. Mismo compromiso que
 * JobStore: válido para dev/single-instance; NO sobrevive reinicios ni escala
 * horizontal. El swap a un store durable (R2DBC/Redis) solo requiere este
 * adapter - el port ya está en su sitio.
 */
@Component
@org.springframework.context.annotation.Profile("local")
public class InMemoryAddMediaProcessRepository implements AddMediaProcessRepository, AddMediaCompensationRepository {

  private final Map<AddMediaId, AddMediaProcess> processes = new ConcurrentHashMap<>();
  private final Map<String, AddMediaId> idempotencyIndex = new ConcurrentHashMap<>();
  private final Map<String, String> fingerprints = new ConcurrentHashMap<>();
  private final Map<Long, Task> compensationTasks = new ConcurrentHashMap<>();
  private final AtomicLong compensationSequence = new AtomicLong();

  @Override
  public Mono<AddMediaProcess> createIfAbsent(
      String ownerSubject, String idempotencyKey, String requestFingerprint) {
    return Mono.defer(() -> {
      String key = ownerSubject + ":" + idempotencyKey;
      String existingFingerprint = this.fingerprints.putIfAbsent(key, requestFingerprint);
      if (existingFingerprint != null && !existingFingerprint.equals(requestFingerprint)) {
        return Mono.error(new com.guille.media.bff.experience.addmedia.application.
            IdempotencyConflictException(idempotencyKey));
      }
      AtomicReference<AddMediaId> resolved = new AtomicReference<>();
      this.idempotencyIndex.compute(key, (k, existingId) -> {
        if (existingId != null) {
          resolved.set(existingId);
          return existingId;
        }
        AddMediaId fresh = AddMediaId.newId();
        this.processes.put(fresh, AddMediaProcess.starting(fresh, ownerSubject));
        resolved.set(fresh);
        return fresh;
      });
      return Mono.justOrEmpty(this.processes.get(resolved.get()));
    });
  }

  @Override
  public Mono<AddMediaProcess> findById(AddMediaId id) {
    return Mono.justOrEmpty(this.processes.get(id));
  }

  @Override
  public Mono<Boolean> tryClaim(AddMediaId id) {
    return Mono.defer(() -> Mono.just(this.claimInPlace(id)));
  }

  private boolean claimInPlace(AddMediaId id) {
    AtomicReference<Boolean> claimed = new AtomicReference<>(false);
    this.processes.compute(id, (k, existing) -> {
      if (existing == null || existing.phase() != com.guille.media.bff.experience.addmedia.model.AddMediaPhase.STARTING) {
        return existing;
      }
      claimed.set(true);
      return existing.preparing();
    });
    return claimed.get();
  }

  @Override
  public Mono<Boolean> tryFinalizeClaim(AddMediaId id) {
    return Mono.defer(() -> {
      AtomicReference<Boolean> claimed = new AtomicReference<>(false);
      var ACTIVE = java.util.Set.of(
          com.guille.media.bff.experience.addmedia.model.AddMediaPhase.WAITING_FOR_UPLOAD,
          com.guille.media.bff.experience.addmedia.model.AddMediaPhase.VERIFYING_UPLOAD);
      this.processes.compute(id, (k, existing) -> {
        if (existing == null || !ACTIVE.contains(existing.phase())) {
          return existing;
        }
        claimed.set(true);
        return existing.finalizing();
      });
      return Mono.just(claimed.get());
    });
  }

  @Override
  public Mono<Boolean> tryCancelClaim(AddMediaId id) {
    return Mono.defer(() -> {
      AtomicReference<Boolean> claimed = new AtomicReference<>(false);
      var ACTIVE = java.util.Set.of(
          com.guille.media.bff.experience.addmedia.model.AddMediaPhase.WAITING_FOR_UPLOAD,
          com.guille.media.bff.experience.addmedia.model.AddMediaPhase.VERIFYING_UPLOAD);
      this.processes.compute(id, (k, existing) -> {
        if (existing == null || !ACTIVE.contains(existing.phase())) {
          return existing;
        }
        claimed.set(true);
        return existing.cancelling();
      });
      return Mono.just(claimed.get());
    });
  }

  @Override
  public Mono<Boolean> tryCompleteCancellation(AddMediaId id) {
    return Mono.defer(() -> {
      AtomicReference<Boolean> completed = new AtomicReference<>(false);
      this.processes.compute(id, (key, existing) -> {
        boolean pending = this.compensationTasks.values().stream()
            .anyMatch(task -> task.processId().equals(id));
        if (existing == null || existing.phase() !=
            com.guille.media.bff.experience.addmedia.model.AddMediaPhase.CANCELLING || pending) {
          return existing;
        }
        completed.set(true);
        return new AddMediaProcess(existing.id(), existing.ownerSubject(), existing.movieId(),
            existing.uploadId(), com.guille.media.bff.experience.addmedia.model.AddMediaPhase.CANCELLED,
            null, existing.version() + 1);
      });
      return Mono.just(completed.get());
    });
  }

  @Override
  public Mono<Boolean> completePreparingRecovery(AddMediaId id) {
    return Mono.defer(() -> {
      AtomicReference<Boolean> completed = new AtomicReference<>(false);
      this.processes.compute(id, (key, existing) -> {
        boolean pending = this.compensationTasks.values().stream()
            .anyMatch(task -> task.processId().equals(id));
        if (existing == null || existing.phase() !=
            com.guille.media.bff.experience.addmedia.model.AddMediaPhase.PREPARING || pending) {
          return existing;
        }
        if (existing.uploadId() != null || existing.movieId() != null) {
          return existing;
        }
        completed.set(true);
        var next = existing.movieId() == null && existing.uploadId() == null
            ? com.guille.media.bff.experience.addmedia.model.AddMediaPhase.STARTING
            : existing.phase();
        return new AddMediaProcess(existing.id(), existing.ownerSubject(), existing.movieId(),
            existing.uploadId(), next, null, existing.version() + 1);
      });
      return Mono.just(completed.get());
    });
  }

  @Override
  public Mono<Boolean> completePreparingRecovery(AddMediaId id, boolean uploadConfirmedAbsent) {
    if (!uploadConfirmedAbsent) return Mono.just(false);
    return Mono.defer(() -> {
      AtomicReference<Boolean> completed = new AtomicReference<>(false);
      this.processes.compute(id, (key, existing) -> {
        boolean pending = this.compensationTasks.values().stream()
            .anyMatch(task -> task.processId().equals(id));
        if (existing == null || pending || existing.phase() !=
            com.guille.media.bff.experience.addmedia.model.AddMediaPhase.PREPARING
            || existing.movieId() == null || existing.uploadId() != null) return existing;
        completed.set(true);
        return new AddMediaProcess(existing.id(), existing.ownerSubject(), existing.movieId(), null,
            com.guille.media.bff.experience.addmedia.model.AddMediaPhase.CANCELLED,
            null, existing.version() + 1);
      });
      return Mono.just(completed.get());
    });
  }

  @Override
  public Mono<Boolean> claimRecoveredCancellation(AddMediaId id, long version, Long uploadId) {
    return Mono.defer(() -> {
      AtomicReference<Boolean> claimed = new AtomicReference<>(false);
      this.processes.compute(id, (key, existing) -> {
        if (existing == null || existing.version() != version
            || existing.phase() != com.guille.media.bff.experience.addmedia.model.AddMediaPhase.PREPARING
            || existing.movieId() == null || existing.uploadId() != null) {
          return existing;
        }
        claimed.set(true);
        return new AddMediaProcess(existing.id(), existing.ownerSubject(), existing.movieId(), uploadId,
            com.guille.media.bff.experience.addmedia.model.AddMediaPhase.CANCELLING,
            null, existing.version() + 1);
      });
      return Mono.just(claimed.get());
    });
  }

  @Override
  public Mono<AddMediaProcess> releaseClaim(AddMediaId id) {
    return Mono.defer(() -> Mono.justOrEmpty(this.releaseInPlace(id)));
  }

  private AddMediaProcess releaseInPlace(AddMediaId id) {
    AtomicReference<AddMediaProcess> released = new AtomicReference<>();
    this.processes.compute(id, (k, existing) -> {
      if (existing == null || existing.phase() != com.guille.media.bff.experience.addmedia.model.AddMediaPhase.PREPARING) {
        return existing;
      }
      AddMediaProcess reverted = existing.revertToStarting();
      released.set(reverted);
      return reverted;
    });
    return released.get();
  }

  @Override
  public Mono<AddMediaProcess> save(AddMediaProcess process) {
    return Mono.defer(() -> {
      AtomicReference<AddMediaProcess> saved = new AtomicReference<>();
      this.processes.compute(process.id(), (id, existing) -> {
        if (existing != null && existing.version() != process.version() - 1) {
          throw new ConcurrentProcessUpdateException(process.id());
        }
        saved.set(process);
        return process;
      });
      return Mono.justOrEmpty(saved.get());
    });
  }

  @Override
  public Mono<Void> enqueue(AddMediaId processId, Kind kind, Long resourceId, Throwable error) {
    return Mono.fromRunnable(() -> compensationTasks.computeIfAbsent(
        processId.value().hashCode() * 31L + kind.hashCode() * 17L + resourceId,
        ignored -> new Task(compensationSequence.incrementAndGet(), processId, kind, resourceId, 0,
            error == null ? null : error.getMessage())));
  }

  @Override
  public Flux<Task> claimPending(int limit) {
    return Flux.fromIterable(compensationTasks.values()).take(limit)
        .map(task -> new Task(task.id(), task.processId(), task.kind(), task.resourceId(),
            task.attempts() + 1, task.lastError()));
  }

  @Override
  public Mono<Void> markCompleted(long taskId) {
    return Mono.fromRunnable(() -> compensationTasks.values().stream()
        .filter(task -> task.id() == taskId).findFirst()
        .ifPresent(task -> compensationTasks.remove(taskKey(task))));
  }

  @Override
  public Mono<Void> markFailed(long taskId, int attempts, Throwable error) {
    return Mono.fromRunnable(() -> compensationTasks.values().stream()
        .filter(task -> task.id() == taskId).findFirst()
        .ifPresent(task -> compensationTasks.put(taskKey(task), new Task(task.id(), task.processId(),
            task.kind(), task.resourceId(), attempts, error.getMessage()))));
  }

  private static long taskKey(Task task) {
    return task.processId().value().hashCode() * 31L + task.kind().hashCode() * 17L + task.resourceId();
  }
}
