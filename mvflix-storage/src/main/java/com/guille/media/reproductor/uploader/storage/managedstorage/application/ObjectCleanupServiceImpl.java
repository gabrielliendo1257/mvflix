package com.guille.media.reproductor.uploader.storage.managedstorage.application;

import com.guille.media.reproductor.uploader.storage.shared.security.UserProvider;
import com.guille.media.reproductor.uploader.storage.managedstorage.application.command.request.DeleteStoredObjectCommand;
import com.guille.media.reproductor.uploader.storage.managedstorage.domain.exception.IllegalStateTransitionException;
import com.guille.media.reproductor.uploader.storage.managedstorage.domain.exception.StorageObjectNotAvailable;
import com.guille.media.reproductor.uploader.storage.managedstorage.domain.model.BucketName;
import com.guille.media.reproductor.uploader.storage.managedstorage.domain.model.StorageLocation;
import com.guille.media.reproductor.uploader.storage.managedstorage.domain.model.StoreObject;
import com.guille.media.reproductor.uploader.storage.managedstorage.domain.model.StoreObject.StorageSessionStatus;
import com.guille.media.reproductor.uploader.storage.managedstorage.domain.port.ObjectStorageService;
import com.guille.media.reproductor.uploader.storage.managedstorage.domain.port.OrphanCleanupQueue;
import com.guille.media.reproductor.uploader.storage.managedstorage.domain.port.StorageRepository;
import com.guille.media.reproductor.uploader.storage.managedstorage.domain.port.UserStorageRepository;

import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;

import reactor.core.publisher.Mono;

import java.time.Instant;

@Slf4j
@Service
public class ObjectCleanupServiceImpl implements ObjectCleanupService {

  private final UserProvider userProvider;
  private final ObjectStorageService objectStoragePort;
  private final StorageRepository storageRepository;
  private final UserStorageRepository userStorageRepository;
  private final TerminalUploadTransition terminalTransition;
  private final OrphanCleanupQueue orphanCleanupQueue;
  private final DeleteStoredObject deleteStoredObject;

  public ObjectCleanupServiceImpl(
      UserProvider userProvider,
      ObjectStorageService objectStorageService,
      StorageRepository storageRepository,
      UserStorageRepository userStorageRepository,
      TerminalUploadTransition terminalTransition,
      OrphanCleanupQueue orphanCleanupQueue,
      DeleteStoredObject deleteStoredObject) {
    this.userProvider = userProvider;
    this.objectStoragePort = objectStorageService;
    this.storageRepository = storageRepository;
    this.userStorageRepository = userStorageRepository;
    this.terminalTransition = terminalTransition;
    this.orphanCleanupQueue = orphanCleanupQueue;
    this.deleteStoredObject = deleteStoredObject;
  }

  @Override
  public Mono<Void> deleteObject(Long storageId) {
    // El endpoint valida ownership; la transición física de Storage (S3 DELETE
    // → CAS COMPLETED→DELETED → cuota) vive en DeleteStoredObject, que es
    // idempotente, retry-safe y no conoce Movies ni ownership.
    return this.userProvider
        .getAuthenticatedUser()
        .flatMap(
            user ->
                this.storageRepository
                    .findById(storageId)
                    .switchIfEmpty(
                        Mono.error(
                            new StorageObjectNotAvailable(
                                "Storage object not available: " + storageId)))
                    .flatMap(
                        object -> {
                          object.ensureOwnedBy(user.subject());
                          return this.deleteStoredObject.execute(
                              new DeleteStoredObjectCommand(storageId));
                        }));
  }

  @Override
  public Mono<Long> expireStaleSessions(Instant cutoff) {
    return this.storageRepository
        .findPendingCreatedBefore(cutoff)
        .flatMapSequential(this::expireStaleSession)
        .count();
  }

  /**
   * Expira una sesión PENDING caducada. La transición (CAS sobre PENDING) y la liberación de
   * cuota son atómicas: si la liberación falla la tx revierte el CAS, la sesión sigue PENDING y
   * el scheduler la reintentará. El blob se borra después de confirmar la tx (best effort).
   */
  private Mono<StoreObject> expireStaleSession(StoreObject object) {
    if (!object.expire()) {
      return Mono.empty();
    }
    return this.userStorageRepository
        .findByOwnerUsername(object.getOwnerUsername())
        .flatMap(
            userStorage ->
                this.terminalTransition
                    .transitionAndRelease(object, StorageSessionStatus.PENDING)
                    .flatMap(
                        expired ->
                            this.deleteObjectBestEffort(expired, userStorage.getBucketName())
                                .thenReturn(expired))
                    .onErrorResume(
                        IllegalStateTransitionException.class,
                        race -> {
                          log.warn(
                              "Expire lost a concurrent transition, skipping: uploadId={}, status={}",
                              object.getStorageId(),
                              object.getStorageObjectStatus());
                          return Mono.empty();
                        }));
  }

  /**
   * DELETE best-effort: si falla, la tarea queda DURABLE en la cola de
   * huérfanos y el scheduler la reintenta. Nunca se pierde un blob.
   */
  private Mono<Void> deleteObjectBestEffort(StoreObject object, BucketName bucket) {
    return Mono.<Void>fromRunnable(
            () ->
                this.objectStoragePort.delete(
                    new StorageLocation(bucket, object.getStorageKey())))
        .onErrorResume(
            error -> {
              log.error(
                  "DELETE falló, encolando tarea huérfana: bucket={} key={} cause={}",
                  bucket.bucketName(), object.getStorageKey().key(), error.getMessage());
              return this.orphanCleanupQueue
                  .enqueue(bucket.bucketName(), object.getStorageKey().key(),
                      object.getOwnerUsername(), "DELETE_FAILED")
                  .onErrorResume(enqueueError -> {
                    log.error("add-media: NO se pudo encolar huérfano {}:{}: {}",
                        bucket.bucketName(), object.getStorageKey().key(),
                        enqueueError.getMessage());
                    return Mono.empty();
                  });
            });
  }
}
