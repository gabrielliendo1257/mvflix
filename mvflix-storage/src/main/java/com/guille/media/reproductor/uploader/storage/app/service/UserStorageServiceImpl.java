package com.guille.media.reproductor.uploader.storage.app.service;

import com.guille.media.reproductor.uploader.storage.app.security.UserProvider;
import com.guille.media.reproductor.uploader.storage.domain.exceptions.UserStorageNotFoundException;
import com.guille.media.reproductor.uploader.storage.domain.models.StorageQuota;
import com.guille.media.reproductor.uploader.storage.domain.models.StorageUsage;
import com.guille.media.reproductor.uploader.storage.domain.models.UserStorage;
import com.guille.media.reproductor.uploader.storage.domain.ports.ObjectStorageService;
import com.guille.media.reproductor.uploader.storage.domain.ports.UserStorageRepository;
import com.guille.media.reproductor.uploader.storage.domain.vos.BucketName;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import reactor.core.publisher.Mono;

@Service
public class UserStorageServiceImpl implements UserStorageService {

  private final UserProvider userProvider;
  private final UserStorageRepository userStorageRepository;
  private final ObjectStorageService objectStoragePort;

  @Value("${minio.bucket}")
  private String usersBucket;

  public UserStorageServiceImpl(
      UserProvider userProvider,
      UserStorageRepository userStorageRepository,
      ObjectStorageService objectStorageService) {
    this.userProvider = userProvider;
    this.userStorageRepository = userStorageRepository;
    this.objectStoragePort = objectStorageService;
  }

  @Override
  public Mono<UserStorage> getUserStorage() {
    return this.userProvider
        .getAuthenticatedUser()
        .flatMap(user -> this.userStorageRepository.findByOwnerUsername(user.subject()))
        .switchIfEmpty(
            Mono.error(new UserStorageNotFoundException("No storage registered for the user")));
  }

  @Override
  public Mono<UserStorage> getUserStorageBy(String username) {
    return this.userStorageRepository
        .findByOwnerUsername(username)
        .switchIfEmpty(
            Mono.error(
                new UserStorageNotFoundException(
                    "No storage registered for the user " + username)));
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
}