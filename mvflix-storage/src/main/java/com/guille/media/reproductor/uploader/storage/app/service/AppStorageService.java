package com.guille.media.reproductor.uploader.storage.app.service;

import com.guille.media.reproductor.uploader.storage.app.commands.requests.CreateUploadCommand;
import com.guille.media.reproductor.uploader.storage.app.commands.requests.StreamingCommand;
import com.guille.media.reproductor.uploader.storage.app.commands.response.StreamingSession;
import com.guille.media.reproductor.uploader.storage.app.commands.response.UploadSession;
import com.guille.media.reproductor.uploader.storage.app.security.AuthenticatedUser;
import com.guille.media.reproductor.uploader.storage.app.security.UserProvider;
import com.guille.media.reproductor.uploader.storage.domain.events.UploadCompletedEvent;
import com.guille.media.reproductor.uploader.storage.domain.exceptions.BucketNotFoundException;
import com.guille.media.reproductor.uploader.storage.domain.exceptions.ExceededQuotaException;
import com.guille.media.reproductor.uploader.storage.domain.exceptions.InvalidObjectContentError;
import com.guille.media.reproductor.uploader.storage.domain.exceptions.StorageObjectNotAvailable;
import com.guille.media.reproductor.uploader.storage.domain.exceptions.UserStorageNotFoundException;
import com.guille.media.reproductor.uploader.storage.domain.models.ExpectedObjectData;
import com.guille.media.reproductor.uploader.storage.domain.models.StorageQuota;
import com.guille.media.reproductor.uploader.storage.domain.models.StorageUsage;
import com.guille.media.reproductor.uploader.storage.domain.models.StoreObject;
import com.guille.media.reproductor.uploader.storage.domain.models.StoreObject.StorageSessionStatus;
import com.guille.media.reproductor.uploader.storage.domain.models.StorageKeyGenerator;
import com.guille.media.reproductor.uploader.storage.domain.models.UploadConfiguration;
import com.guille.media.reproductor.uploader.storage.domain.models.UserStorage;
import com.guille.media.reproductor.uploader.storage.domain.ports.ObjectStorageService;
import com.guille.media.reproductor.uploader.storage.domain.ports.StorageEventPublisher;
import com.guille.media.reproductor.uploader.storage.domain.ports.StorageRepository;
import com.guille.media.reproductor.uploader.storage.domain.ports.UserStorageRepository;
import com.guille.media.reproductor.uploader.storage.domain.service.StorageService;
import com.guille.media.reproductor.uploader.storage.domain.service.UploadPolicy;
import com.guille.media.reproductor.uploader.storage.domain.vos.BucketName;
import com.guille.media.reproductor.uploader.storage.domain.vos.MimeType;
import com.guille.media.reproductor.uploader.storage.domain.vos.PresignedUploadRequest;
import com.guille.media.reproductor.uploader.storage.domain.vos.StorageFolder;
import com.guille.media.reproductor.uploader.storage.domain.vos.StorageKey;
import com.guille.media.reproductor.uploader.storage.domain.vos.StorageLocation;
import com.guille.media.reproductor.uploader.storage.domain.vos.StorageMetadata;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.stereotype.Service;

import reactor.core.publisher.Mono;

import java.time.Instant;

@Slf4j
@Service
public class AppStorageService implements StorageService {

  private final ObjectStorageService objectStoragePort;
  private final StorageKeyGenerator storageKeyGenerator;
  private final UploadPolicy uploadPolicy;
  private final StorageRepository storageRepository;
  private final UserProvider userProvider;
  private final UserStorageRepository userStorageRepository;
  private final StorageEventPublisher eventPublisher;

  @Value("${minio.bucket}")
  private String usersBucket;

  public AppStorageService(
      ObjectStorageService objectStorageService,
      StorageKeyGenerator storageKeyGenerator,
      UploadPolicy uploadPolicy,
      StorageRepository storageRepository,
      UserProvider userProvider,
      UserStorageRepository userStorageRepository,
      StorageEventPublisher eventPublisher) {
    this.objectStoragePort = objectStorageService;
    this.storageKeyGenerator = storageKeyGenerator;
    this.uploadPolicy = uploadPolicy;
    this.storageRepository = storageRepository;
    this.userProvider = userProvider;
    this.userStorageRepository = userStorageRepository;
    this.eventPublisher = eventPublisher;
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
                this.userStorageRepository
                    .consumeStorage(userStorage.getOwnerUsername(), command.size())
                    .filter(rowsUpdated -> rowsUpdated == 1)
                    .switchIfEmpty(
                        Mono.error(
                            new ExceededQuotaException(
                                "Storage quota exceeded for user: "
                                    + userStorage.getOwnerUsername())))
                    .then(this.storageRepository.save(object))
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
  public Mono<StreamingSession> generateStreamingSession(StreamingCommand command) {
    return Mono.fromCallable(() -> Long.parseLong(command.objectId()))
        .onErrorMap(
            NumberFormatException.class,
            ex -> new IllegalArgumentException("Invalid objectId: " + command.objectId()))
        .flatMap(this.storageRepository::findById)
        .switchIfEmpty(
            Mono.error(
                new StorageObjectNotAvailable(
                    "Storage object not available: " + command.objectId())))
        .flatMap(this::createStreamingSession)
        .doOnNext(
            session -> log.info("Streaming session created: uploadId={}", session.uploadId()));
  }

  private Mono<StreamingSession> createStreamingSession(StoreObject object) {
    object.ensureAvailable();

    UploadConfiguration configuration =
        this.uploadPolicy.resolve(object.sizeInBytes(), MimeType.of(object.contentType()));

    return this.userStorageRepository
        .findByOwnerUsername(object.getOwnerUsername())
        .switchIfEmpty(
            Mono.error(
                new UserStorageNotFoundException(
                    "No storage registered for user: " + object.getOwnerUsername())))
        .flatMap(
            userStorage -> {
              StorageLocation location =
                  new StorageLocation(userStorage.getBucketName(), object.getStorageKey());
              PresignedUploadRequest request =
                  new PresignedUploadRequest(configuration.expiration());

              return this.objectStoragePort
                  .createStreamingUrl(request, location)
                  .flatMap(
                      permissionUrl ->
                          this.storageRepository
                              .touchLastSeen(object.getStorageId(), Instant.now())
                              .onErrorResume(
                                  error -> {
                                    log.warn(
                                        "Failed to record last-seen for storageId={}",
                                        object.getStorageId(),
                                        error);
                                    return Mono.empty();
                                  })
                              .thenReturn(
                                  new StreamingSession(
                                      String.valueOf(object.getStorageId()),
                                      permissionUrl.presignedUrl(),
                                      object.getStorageKey(),
                                      Instant.now().plus(configuration.expiration()),
                                      permissionUrl.method())));
            });
  }

  @Override
  public Mono<Void> completeUpload(Long uploadId) {
    log.info("Completing upload: uploadId={}", uploadId);

    return this.storageRepository
        .findById(uploadId)
        .switchIfEmpty(
            Mono.error(new StorageObjectNotAvailable("Storage object not available: " + uploadId)))
        .flatMap(
            object ->
                this.userStorageRepository
                    .findByOwnerUsername(object.getOwnerUsername())
                    .switchIfEmpty(
                        Mono.error(
                            new UserStorageNotFoundException(
                                "No storage registered for user: " + object.getOwnerUsername())))
                    .flatMap(
                        userStorage ->
                            this.validateUploadedObject(object, userStorage.getBucketName())))
        .doOnNext(
            completed ->
                this.eventPublisher.publish(
                    new UploadCompletedEvent(
                        completed.getStorageId(),
                        completed.getOwnerUsername(),
                        completed.getStorageKey().key(),
                        completed.contentType(),
                        completed.sizeInBytes(),
                        Instant.now())))
        .then();
  }

  private Mono<StoreObject> validateUploadedObject(StoreObject object, BucketName bucket) {
    StorageLocation location = new StorageLocation(bucket, object.getStorageKey());

    return this.objectStoragePort
        .objectExists(location)
        .flatMap(
            exists -> {
              if (!exists) {
                return this.releaseAndFail(
                    object,
                    new StorageObjectNotAvailable(
                        "Storage object not available: " + object.getStorageId()));
              }
              return this.objectStoragePort.getMetadata(location);
            })
        .flatMap(
            metadata ->
                Mono.fromCallable(
                        () -> {
                          object.ensureValidContentLength(metadata.contentLength());
                          return object.complete();
                        })
                    .onErrorResume(
                        InvalidObjectContentError.class,
                        contentError -> this.releaseAndFail(object, contentError))
                    .flatMap(
                        transitioned ->
                            transitioned
                                ? this.storageRepository.updateStatus(
                                    object, StorageSessionStatus.PENDING)
                                : Mono.just(object)));
  }

  private <T> Mono<T> releaseAndFail(StoreObject object, RuntimeException error) {
    return this.userStorageRepository
        .releaseStorage(object.getOwnerUsername(), object.sizeInBytes())
        .then(Mono.error(error));
  }

  @Override
  public Mono<Long> expireStaleSessions(Instant cutoff) {
    return this.storageRepository
        .findPendingCreatedBefore(cutoff)
        .flatMapSequential(
            object -> {
              if (!object.expire()) {
                return Mono.empty();
              }
              return this.userStorageRepository
                  .releaseStorage(object.getOwnerUsername(), object.sizeInBytes())
                  .then(this.storageRepository.updateStatus(object, StorageSessionStatus.PENDING));
            })
        .count();
  }

  @Override
  public Mono<UserStorage> getUserStorage() {
    return this.authenticatedUser()
        .flatMap(user -> this.userStorageRepository.findByOwnerUsername(user.subject()))
        .switchIfEmpty(
            Mono.error(new UserStorageNotFoundException("No storage registered for the user")));
  }

  @Override
  public Mono<Void> deleteObject(Long storageId) {
    return this.authenticatedUser()
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
                          return this.deleteOwnedObject(object);
                        }));
  }

  private Mono<Void> deleteOwnedObject(StoreObject object) {
    return this.userStorageRepository
        .findByOwnerUsername(object.getOwnerUsername())
        .switchIfEmpty(
            Mono.error(
                new UserStorageNotFoundException(
                    "No storage registered for user: " + object.getOwnerUsername())))
        .flatMap(
            userStorage -> {
              if (!object.markDeleted()) {
                return Mono.empty();
              }
              return Mono.fromRunnable(
                      () ->
                          this.objectStoragePort.delete(
                              new StorageLocation(
                                  userStorage.getBucketName(), object.getStorageKey())))
                  .then(
                      this.userStorageRepository.releaseStorage(
                          object.getOwnerUsername(), object.sizeInBytes()))
                  .then(
                      this.storageRepository.updateStatus(
                          object, StorageSessionStatus.COMPLETED))
                  .then();
            });
  }

  @Override
  public Mono<Void> ensureUserStorage(String username, long quotaBytes) {
    return this.userStorageRepository
        .findByOwnerUsername(username)
        .switchIfEmpty(
            Mono.defer(
                () ->
                    this.userStorageRepository.save(
                        new UserStorage(
                            null,
                            BucketName.of(this.usersBucket),
                            username,
                            new StorageQuota(quotaBytes),
                            new StorageUsage(0)))))
        .flatMap(
            userStorage ->
                this.objectStoragePort.ensureUserStorageLayout(
                    userStorage.getBucketName(), username))
        .then();
  }

  private Mono<AuthenticatedUser> authenticatedUser() {
    return this.userProvider.getAuthenticatedUser();
  }
}
