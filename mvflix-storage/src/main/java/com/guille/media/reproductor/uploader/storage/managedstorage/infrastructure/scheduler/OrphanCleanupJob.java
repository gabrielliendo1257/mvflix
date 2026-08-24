package com.guille.media.reproductor.uploader.storage.managedstorage.infrastructure.scheduler;

import com.guille.media.reproductor.uploader.storage.managedstorage.domain.model.BucketName;
import com.guille.media.reproductor.uploader.storage.managedstorage.domain.model.StorageKey;
import com.guille.media.reproductor.uploader.storage.managedstorage.domain.model.StorageLocation;
import com.guille.media.reproductor.uploader.storage.managedstorage.domain.port.ObjectStorageService;
import com.guille.media.reproductor.uploader.storage.managedstorage.domain.port.OrphanCleanupQueue;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import reactor.core.publisher.Mono;

/**
 * Reintentos DURABLES de huérfanos: toma tareas pendientes de la cola,
 * borra el blob y marca la tarea procesada. Un fallo deja la tarea pendiente
 * para la próxima corrida. Igual que SessionExpirationJob, corre en el
 * scheduler compartido; cada corrida es acotada por batch-size.
 */
@Slf4j
@Component
public class OrphanCleanupJob {

  private final OrphanCleanupQueue queue;
  private final ObjectStorageService objectStorage;
  private final int batchSize;

  public OrphanCleanupJob(
      OrphanCleanupQueue queue,
      ObjectStorageService objectStorage,
      @Value("${storage.orphan.batch-size:50}") int batchSize) {
    this.queue = queue;
    this.objectStorage = objectStorage;
    this.batchSize = batchSize;
  }

  @Scheduled(fixedDelayString = "${storage.orphan.check-ms:900000}")
  public void processPending() {
    this.queue
        .pending(this.batchSize)
        .flatMap(this::deleteAndMark, 1)
        .doOnComplete(() -> log.debug("Orphan cleanup pass terminada"))
        .subscribe(
            ok -> {},
            error -> log.error("Orphan cleanup pass abortada: {}", error.getMessage()));
  }

  private Mono<Void> deleteAndMark(OrphanCleanupQueue.OrphanTask task) {
    var location =
        new StorageLocation(BucketName.of(task.bucket()), new StorageKey(task.objectKey()));
    return Mono.<Void>fromRunnable(() -> this.objectStorage.delete(location))
        .then(this.queue.markProcessed(task.id()))
        .doOnSuccess(ok -> log.info("Huérfano eliminado: {}:{}", task.bucket(), task.objectKey()))
        .onErrorResume(error -> {
          log.warn("Reintento fallido {}:{}: {}", task.bucket(), task.objectKey(),
              error.getMessage());
          return Mono.empty();
        });
  }
}
