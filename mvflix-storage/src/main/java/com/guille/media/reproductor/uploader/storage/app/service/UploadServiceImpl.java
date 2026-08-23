package com.guille.media.reproductor.uploader.storage.app.service;

import com.guille.media.reproductor.uploader.storage.app.commands.requests.CreateUploadCommand;
import com.guille.media.reproductor.uploader.storage.app.commands.response.UploadSession;
import com.guille.media.reproductor.uploader.storage.app.commands.response.UploadCompletionResult;
import com.guille.media.reproductor.uploader.storage.app.commands.response.UploadSummary;
import com.guille.media.reproductor.uploader.storage.shared.security.UserProvider;
import com.guille.media.reproductor.uploader.storage.domain.events.UploadCompletedEvent;
import com.guille.media.reproductor.uploader.storage.domain.events.UploadFailedEvent;
import com.guille.media.reproductor.uploader.storage.domain.exceptions.BucketNotFoundException;
import com.guille.media.reproductor.uploader.storage.domain.exceptions.ExceededQuotaException;
import com.guille.media.reproductor.uploader.storage.domain.exceptions.IllegalStateTransitionException;
import com.guille.media.reproductor.uploader.storage.domain.exceptions.InvalidObjectContentError;
import com.guille.media.reproductor.uploader.storage.domain.exceptions.StorageObjectNotAvailable;
import com.guille.media.reproductor.uploader.storage.domain.exceptions.StorageObjectRemovedException;
import com.guille.media.reproductor.uploader.storage.domain.exceptions.UploadCancelledByUserException;
import com.guille.media.reproductor.uploader.storage.domain.exceptions.UserStorageNotFoundException;
import com.guille.media.reproductor.uploader.storage.domain.models.ExpectedObjectData;
import com.guille.media.reproductor.uploader.storage.domain.models.StorageKeyGenerator;
import com.guille.media.reproductor.uploader.storage.domain.models.StoreObject;
import com.guille.media.reproductor.uploader.storage.domain.models.StoreObject.StorageSessionStatus;
import com.guille.media.reproductor.uploader.storage.domain.models.UploadConfiguration;
import com.guille.media.reproductor.uploader.storage.domain.models.UserStorage;
import com.guille.media.reproductor.uploader.storage.domain.ports.ObjectStorageService;
import com.guille.media.reproductor.uploader.storage.domain.ports.StorageEventPublisher;
import com.guille.media.reproductor.uploader.storage.domain.ports.StorageRepository;
import com.guille.media.reproductor.uploader.storage.domain.ports.UserStorageRepository;
import com.guille.media.reproductor.uploader.storage.domain.service.UploadPolicy;
import com.guille.media.reproductor.uploader.storage.domain.vos.BucketName;
import com.guille.media.reproductor.uploader.storage.domain.vos.PresignedUploadRequest;
import com.guille.media.reproductor.uploader.storage.domain.vos.StorageFolder;
import com.guille.media.reproductor.uploader.storage.domain.vos.StorageKey;
import com.guille.media.reproductor.uploader.storage.domain.vos.StorageLocation;
import com.guille.media.reproductor.uploader.storage.domain.vos.StorageMetadata;

import lombok.extern.slf4j.Slf4j;

import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.reactive.TransactionalOperator;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;

@Slf4j
@Service
public class UploadServiceImpl implements UploadService {

  private final ObjectStorageService objectStoragePort;
  private final StorageKeyGenerator storageKeyGenerator;
  private final UploadPolicy uploadPolicy;
  private final StorageRepository storageRepository;
  private final UserProvider userProvider;
  private final UserStorageRepository userStorageRepository;
  private final StorageEventPublisher eventPublisher;
  private final TransactionalOperator transactionalOperator;
  private final TerminalUploadTransition terminalTransition;

  public UploadServiceImpl(
      ObjectStorageService objectStorageService,
      StorageKeyGenerator storageKeyGenerator,
      UploadPolicy uploadPolicy,
      StorageRepository storageRepository,
      UserProvider userProvider,
      UserStorageRepository userStorageRepository,
      StorageEventPublisher eventPublisher,
      TransactionalOperator transactionalOperator,
      TerminalUploadTransition terminalTransition) {
    this.objectStoragePort = objectStorageService;
    this.storageKeyGenerator = storageKeyGenerator;
    this.uploadPolicy = uploadPolicy;
    this.storageRepository = storageRepository;
    this.userProvider = userProvider;
    this.userStorageRepository = userStorageRepository;
    this.eventPublisher = eventPublisher;
    this.transactionalOperator = transactionalOperator;
    this.terminalTransition = terminalTransition;
  }

  @Override
  public Mono<UploadSession> createUploadSession(CreateUploadCommand command) {
    log.info(
        "Creating upload session: filename={}, size={}, mimeType={}",
        command.filename(),
        command.size(),
        command.mimeType());

    return this.userProvider
        .getAuthenticatedUser()
        .switchIfEmpty(
            Mono.error(new AuthenticationCredentialsNotFoundException("No authenticated user")))
        .flatMap(user -> this.userStorageRepository.findByOwnerUsername(user.subject()))
        .switchIfEmpty(
            Mono.error(new UserStorageNotFoundException("No storage registered for the user")))
        .flatMap(
            userStorage ->
                this.objectStoragePort
                    .bucketExists(userStorage.getBucketName())
                    .flatMap(
                        exists -> {
                          if (!exists) {
                            return Mono.error(
                                new BucketNotFoundException(
                                    "Bucket not found: " + userStorage.getBucketName()));
                          }
                          return this.createSession(userStorage, command);
                        }))
        .doOnNext(session -> log.info("Upload session created: uploadId={}", session.uploadId()));
  }

  private Mono<UploadSession> createSession(UserStorage userStorage, CreateUploadCommand command) {
    UploadConfiguration configuration =
        this.uploadPolicy.resolve(command.size(), command.mimeType());

    StorageKey key =
        this.storageKeyGenerator.generate(
            userStorage.getOwnerUsername(), StorageFolder.from(command.mimeType()));
    StorageLocation location = new StorageLocation(userStorage.getBucketName(), key);

    PresignedUploadRequest presignedRequest =
        new PresignedUploadRequest(configuration.expiration());
    presignedRequest.setContentType(command.mimeType());

    StorageMetadata metadata =
        new StorageMetadata(command.mimeType().value(), command.size(), null, Instant.now());
    StoreObject object =
        new StoreObject(
            userStorage.getOwnerUsername(),
            key,
            metadata,
            Instant.now(),
            null,
            StorageSessionStatus.PENDING);

    return this.objectStoragePort
        .createUploadUrl(presignedRequest, location)
        .flatMap(
            permissionUrl ->
                // Reserva de cuota + persistencia de la sesión comparten una única
                // transacción local: si el save falla, el consumo se revierte y no
                // quedan bytes reservados sin fila que los libere.
                this.transactionalOperator
                    .transactional(
                        this.userStorageRepository
                            .consumeStorage(userStorage.getOwnerUsername(), command.size())
                            .filter(rowsUpdated -> rowsUpdated == 1)
                            .switchIfEmpty(
                                Mono.error(
                                    new ExceededQuotaException(
                                        "Storage quota exceeded for user: "
                                            + userStorage.getOwnerUsername())))
                            .then(this.storageRepository.save(object)))
                    .map(
                        saved ->
                            new UploadSession(
                                String.valueOf(saved.getStorageId()),
                                permissionUrl.presignedUrl(),
                                key,
                                permissionUrl.method(),
                                Instant.now().plus(configuration.expiration()),
                                saved.getStorageObjectStatus(),
                                new ExpectedObjectData(
                                    saved.sizeInBytes(), command.mimeType().value()))));
  }

  @Override
  public Mono<Void> handleObjectRemoved(String objectKey) {
    log.info("Object removed event received from object store: key={}", objectKey);

    return this.storageRepository
        .findByObjectKey(objectKey)
        .flatMap(
            object -> {
              if (object.getStorageObjectStatus() != StorageSessionStatus.PENDING) {
                log.info(
                    "Object removed but session is not PENDING, skipping: key={}, status={}",
                    objectKey,
                    object.getStorageObjectStatus());
                return Mono.<StoreObject>empty();
              }
              object.markFailed();
              // El segundo parámetro de updateStatus es el estado ANTERIOR
              // esperado en la fila (CAS). La fila sigue PENDING: si se
              // esperara FAILED el CAS nunca matchearía. Transición y liberación
              // comparten transacción: o ambas, o ninguna (MinIO ya eliminó el
              // blob; este evento es la reconciliación de ese borrado).
              return this.terminalTransition.transitionAndRelease(
                  object, StorageSessionStatus.PENDING);
            })
        .doOnNext(failed -> this.publishFailed(failed, new StorageObjectRemovedException()))
        .onErrorResume(
            error -> {
              log.warn(
                  "Object removed event could not be reconciled, skipping: key={}, cause={}",
                  objectKey,
                  error.getMessage());
              return Mono.empty();
            })
        .then();
  }

  @Override
  public Flux<UploadSummary> listUploads(int limit) {
    return this.userProvider
        .getAuthenticatedUser()
        .flatMapMany(
            user ->
                this.storageRepository
                    .findRecentByOwner(user.subject(), Math.min(limit, 50))
                    .map(
                        object ->
                            new UploadSummary(
                                object.getStorageId(),
                                object.getStorageKey().key(),
                                object.getStorageObjectStatus(),
                                object.sizeInBytes(),
                                object.getCreatedAt())));
  }

  @Override
  public Mono<Void> cancelUpload(Long uploadId) {
    log.info("Cancelling upload: uploadId={}", uploadId);

    return this.userProvider
        .getAuthenticatedUser()
        .switchIfEmpty(
            Mono.error(new AuthenticationCredentialsNotFoundException("No authenticated user")))
        .flatMap(
            user ->
                this.storageRepository
                    .findById(uploadId)
                    .switchIfEmpty(
                        Mono.error(new StorageObjectNotAvailable(
                            "Storage object not available: " + uploadId)))
                    .doOnNext(object -> object.ensureOwnedBy(user.subject()))
                    .flatMap(
                        object -> {
                          if (object.getStorageObjectStatus() != StorageSessionStatus.PENDING) {
                log.info(
                    "Upload is no longer cancellable (not PENDING), skipping: uploadId={}, status={}",
                    uploadId,
                    object.getStorageObjectStatus());
                return Mono.<StoreObject>empty();
              }
                          object.markFailed();
                          return this.userStorageRepository
                              .findByOwnerUsername(object.getOwnerUsername())
                              .flatMap(
                                  userStorage ->
                                      // Transición + liberación atómicas (ver
                                      // TerminalUploadTransition); el blob se borra
                                      // después del commit, best effort.
                                      this.terminalTransition
                                          .transitionAndRelease(object, StorageSessionStatus.PENDING)
                                          .flatMap(
                                              failed ->
                                                  this.deleteObjectBestEffort(
                                                          failed, userStorage.getBucketName())
                                                      .thenReturn(failed)))
                  .onErrorResume(
                      IllegalStateTransitionException.class,
                      race -> {
                        log.warn(
                            "Cancel lost a concurrent transition, skipping: uploadId={}, status={}",
                            uploadId,
                            object.getStorageObjectStatus());
                        return Mono.empty();
                      });
                        })
                    .doOnNext(
                        failed ->
                            this.publishFailed(failed, new UploadCancelledByUserException())))
        .then();
  }

  @Override
  public Mono<UploadCompletionResult> completeUpload(Long uploadId) {
    log.info("Completing upload: uploadId={}", uploadId);

    return this.userProvider
        .getAuthenticatedUser()
        .switchIfEmpty(
            Mono.error(new AuthenticationCredentialsNotFoundException("No authenticated user")))
        .flatMap(
            user ->
                this.storageRepository
                    .findById(uploadId)
                    .switchIfEmpty(
                        Mono.error(new StorageObjectNotAvailable(
                            "Storage object not available: " + uploadId)))
                    .doOnNext(object -> object.ensureOwnedBy(user.subject()))
                    .flatMap(
                        object ->
                            this.userStorageRepository
                                .findByOwnerUsername(object.getOwnerUsername())
                                .switchIfEmpty(
                                    Mono.error(
                                        new UserStorageNotFoundException(
                                            "No storage registered for user: "
                                                + object.getOwnerUsername())))
                                .flatMap(
                                    userStorage ->
                                        this.completeUploadedObject(
                                            object, userStorage.getBucketName(), true)))
        .doOnNext(
            completion -> {
              if (completion.transitioned()) {
                this.publishCompleted(completion.object());
              } else if (completion.pendingVerification()) {
                log.info(
                    "Upload confirmation arrived before the object exists, "
                        + "leaving session pending for webhook reconciliation: uploadId={}",
                    uploadId);
              } else {
                log.info("Upload already completed, skipping: uploadId={}", uploadId);
              }
            })
                    .map(this::toCompletionResult));
  }

  private UploadCompletionResult toCompletionResult(Completion completion) {
    if (completion.pendingVerification()) {
      return UploadCompletionResult.pendingVerification();
    }
    return UploadCompletionResult.completed();
  }

  @Override
  public Mono<UploadSession> getUploadStatus(Long uploadId) {
    log.info("Querying upload status: uploadId={}", uploadId);

    return this.storageRepository
        .findById(uploadId)
        .switchIfEmpty(
            Mono.error(new StorageObjectNotAvailable("Storage object not available: " + uploadId)))
        .flatMap(
            object ->
                this.userProvider
                    .getAuthenticatedUser()
                    .map(user -> {
                      object.ensureOwnedBy(user.subject());
                      return object;
                    }))
        .map(
            object ->
                new UploadSession(
                    String.valueOf(object.getStorageId()),
                    null,
                    object.getStorageKey(),
                    null,
                    null,
                    object.getStorageObjectStatus(),
                    new ExpectedObjectData(
                        object.sizeInBytes(), object.contentType())));
  }

  @Override
  public Mono<Void> completeUploadByKey(String objectKey) {
    log.info("Reconciling upload from object store event: key={}", objectKey);

    return this.storageRepository
        .findByObjectKey(objectKey)
        .flatMap(
            object -> {
              StorageSessionStatus status = object.getStorageObjectStatus();
              if (status == StorageSessionStatus.EXPIRED
                  || status == StorageSessionStatus.FAILED) {
                log.info(
                    "Object store event for non-active session, removing orphan object: "
                        + "key={}, status={}",
                    objectKey,
                    status);
                return this.cleanupOrphanObject(object).then(Mono.<Completion>empty());
              }
              return this.userStorageRepository
                  .findByOwnerUsername(object.getOwnerUsername())
                  .switchIfEmpty(
                      Mono.error(
                          new UserStorageNotFoundException(
                              "No storage registered for user: "
                                  + object.getOwnerUsername())))
                  .flatMap(
                      userStorage ->
                          this.completeUploadedObject(
                              object, userStorage.getBucketName(), false));
            })
        .doOnNext(
            completion -> {
              if (completion.transitioned()) {
                this.publishCompleted(completion.object());
              } else {
                log.info("Object already completed, skipping event reconciliation: key={}", objectKey);
              }
            })
        .onErrorResume(
            error -> {
              log.warn(
                  "Object store event could not be reconciled, skipping: key={}, cause={}",
                  objectKey,
                  error.getMessage());
              return Mono.empty();
            })
        .then();
  }

  private void publishCompleted(StoreObject completed) {
    this.eventPublisher.publish(
        new UploadCompletedEvent(
            completed.getStorageId(),
            completed.getOwnerUsername(),
            completed.getStorageKey().key(),
            completed.contentType(),
            completed.sizeInBytes(),
            Instant.now()));
  }

  /**
   * Evento tardío de MinIO para una sesión que ya no está activa (EXPIRED/FAILED): el objeto
   * que llegó después de la expiración es basura y se borra del bucket (la fila se conserva
   * como historial; su cuota ya fue liberada).
   */
  private Mono<Void> cleanupOrphanObject(StoreObject object) {
    return this.userStorageRepository
        .findByOwnerUsername(object.getOwnerUsername())
        .flatMap(
            userStorage ->
                this.deleteObjectBestEffort(object, userStorage.getBucketName()))
        .switchIfEmpty(
            Mono.defer(
                () -> {
                  log.warn(
                      "No user storage for orphan cleanup, skipping: uploadId={}",
                      object.getStorageId());
                  return Mono.empty();
                }));
  }

  private Mono<Completion> completeUploadedObject(
      StoreObject object, BucketName bucket, boolean clientConfirmation) {
    StorageLocation location = new StorageLocation(bucket, object.getStorageKey());

    return this.objectStoragePort
        .objectExists(location)
        .flatMap(
            exists -> {
              if (!exists) {
                if (clientConfirmation) {
                  return Mono.just(new Completion(object, false, true));
                }
                return this.releaseAndFail(
                    object,
                    bucket,
                    new StorageObjectNotAvailable(
                        "Storage object not available: " + object.getStorageId()));
              }
              return this.verifyMetadataAndComplete(object, bucket, location);
            });
  }

  private Mono<Completion> verifyMetadataAndComplete(
      StoreObject object, BucketName bucket, StorageLocation location) {
    return this.objectStoragePort
        .getMetadata(location)
        .flatMap(
            metadata ->
                Mono.fromCallable(
                        () -> {
                          object.ensureValidContentLength(metadata.contentLength());
                          return object.complete();
                        })
                    .onErrorResume(
                        InvalidObjectContentError.class,
                        contentError ->
                            this.releaseAndFail(object, bucket, contentError)))
        .flatMap(
            transitioned ->
                transitioned
                    ? this.storageRepository
                        .updateStatus(object, StorageSessionStatus.PENDING)
                        .map(saved -> new Completion(saved, true, false))
                        .onErrorResume(
                            IllegalStateTransitionException.class,
                            race -> this.raceLostToAnotherCompletion(object))
                    : Mono.just(new Completion(object, false, false)));
  }

  /**
   * La transición perdió la carrera contra el otro camino de completado (webhook de MinIO o
   * confirmación del cliente). Si el objeto ya quedó COMPLETED, es el mismo resultado esperado
   * (idempotente); si quedó en otro estado (p.ej. FAILED por cancelación), se propaga el error.
   */
  private Mono<Completion> raceLostToAnotherCompletion(StoreObject object) {
    return this.storageRepository
        .findById(object.getStorageId())
        .flatMap(
            current -> {
              if (current.getStorageObjectStatus() == StorageSessionStatus.COMPLETED) {
                log.info(
                    "Upload already completed by another path (webhook/cliente), "
                        + "treating as success: uploadId={}",
                    current.getStorageId());
                return Mono.just(new Completion(current, false, false));
              }
              return Mono.error(
                  new IllegalStateTransitionException(
                      "Cannot transition object " + current.getStorageId()
                          + ": expected status PENDING in database, concurrent modification"
                          + " detected"));
            });
  }

  private <T> Mono<T> releaseAndFail(
      StoreObject object, BucketName bucket, RuntimeException error) {
    return Mono.defer(
        () -> {
          if (!object.markFailed()) {
            return Mono.error(error);
          }
          // Transición + liberación atómicas; el borrado del blob ocurre
          // después y best effort: un blob huérfano es reconciliable, una
          // cuota descontada dos veces no.
          return this.terminalTransition
              .transitionAndRelease(object, StorageSessionStatus.PENDING)
              .flatMap(failed -> this.deleteObjectBestEffort(failed, bucket).thenReturn(failed))
              .doOnNext(failed -> this.publishFailed(failed, error))
              .onErrorResume(
                  IllegalStateTransitionException.class,
                  race -> {
                    log.warn(
                        "Fail lost a concurrent transition, skipping persist and release: uploadId={}",
                        object.getStorageId());
                    return Mono.error(error);
                  })
              .then(Mono.error(error));
        });
  }

  private void publishFailed(StoreObject failed, RuntimeException error) {
    this.eventPublisher.publish(
        new UploadFailedEvent(
            failed.getStorageId(),
            failed.getOwnerUsername(),
            failed.getStorageKey().key(),
            error.getMessage(),
            Instant.now()));
  }

  /**
   * Borra el objeto del bucket sin romper el flujo (el DELETE de S3 es idempotente). Se usa
   * para limpiar objetos huérfanos de sesiones EXPIRED/FAILED/canceladas.
   */
  private Mono<Void> deleteObjectBestEffort(StoreObject object, BucketName bucket) {
    return Mono.<Void>fromRunnable(
            () ->
                this.objectStoragePort.delete(
                    new StorageLocation(bucket, object.getStorageKey())))
        .onErrorResume(
            error -> {
              log.warn(
                  "Could not delete object from bucket (best effort), uploadId={}, cause={}",
                  object.getStorageId(),
                  error.getMessage());
              return Mono.empty();
            });
  }

  private record Completion(
      StoreObject object, boolean transitioned, boolean pendingVerification) {}
}
