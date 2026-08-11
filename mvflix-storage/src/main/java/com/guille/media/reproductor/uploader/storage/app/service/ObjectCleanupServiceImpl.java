package com.guille.media.reproductor.uploader.storage.app.service;

import com.guille.media.reproductor.uploader.storage.app.security.UserProvider;
import com.guille.media.reproductor.uploader.storage.domain.exceptions.IllegalStateTransitionException;
import com.guille.media.reproductor.uploader.storage.domain.exceptions.StorageObjectNotAvailable;
import com.guille.media.reproductor.uploader.storage.domain.exceptions.UserStorageNotFoundException;
import com.guille.media.reproductor.uploader.storage.domain.models.StoreObject;
import com.guille.media.reproductor.uploader.storage.domain.models.StoreObject.StorageSessionStatus;
import com.guille.media.reproductor.uploader.storage.domain.ports.ObjectStorageService;
import com.guille.media.reproductor.uploader.storage.domain.ports.StorageRepository;
import com.guille.media.reproductor.uploader.storage.domain.ports.UserStorageRepository;
import com.guille.media.reproductor.uploader.storage.domain.service.ObjectCleanupService;
import com.guille.media.reproductor.uploader.storage.domain.vos.StorageLocation;

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

  public ObjectCleanupServiceImpl(
      UserProvider userProvider,
      ObjectStorageService objectStorageService,
      StorageRepository storageRepository,
      UserStorageRepository userStorageRepository) {
    this.userProvider = userProvider;
    this.objectStoragePort = objectStorageService;
    this.storageRepository = storageRepository;
    this.userStorageRepository = userStorageRepository;
  }

  @Override
  public Mono<Void> deleteObject(Long storageId) {
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
  public Mono<Long> expireStaleSessions(Instant cutoff) {
    return this.storageRepository
        .findPendingCreatedBefore(cutoff)
        .flatMapSequential(
            object -> {
              if (!object.expire()) {
                return Mono.empty();
              }
              return this.storageRepository
                  .updateStatus(object, StorageSessionStatus.PENDING)
                  .flatMap(
                      expired ->
                          this.userStorageRepository.releaseStorage(
                              expired.getOwnerUsername(), expired.sizeInBytes()))
                  .onErrorResume(
                      IllegalStateTransitionException.class,
                      race -> {
                        log.warn(
                            "Expire lost a concurrent transition, skipping: uploadId={}, status={}",
                            object.getStorageId(),
                            object.getStorageObjectStatus());
                        return Mono.empty();
                      });
            })
        .count();
  }
}