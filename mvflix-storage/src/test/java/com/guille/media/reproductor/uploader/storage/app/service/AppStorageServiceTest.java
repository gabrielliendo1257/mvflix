package com.guille.media.reproductor.uploader.storage.app.service;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.guille.media.reproductor.uploader.storage.app.security.AuthenticatedUser;
import com.guille.media.reproductor.uploader.storage.app.security.UserProvider;
import com.guille.media.reproductor.uploader.storage.app.user.UserServiceCommandPort;
import com.guille.media.reproductor.uploader.storage.domain.exceptions.StorageObjectNotAvailable;
import com.guille.media.reproductor.uploader.storage.domain.exceptions.UserStorageNotFoundException;
import com.guille.media.reproductor.uploader.storage.domain.models.StorageKeyGenerator;
import com.guille.media.reproductor.uploader.storage.domain.models.StorageQuota;
import com.guille.media.reproductor.uploader.storage.domain.models.StorageUsage;
import com.guille.media.reproductor.uploader.storage.domain.models.StoreObject;
import com.guille.media.reproductor.uploader.storage.domain.models.StoreObject.StorageSessionStatus;
import com.guille.media.reproductor.uploader.storage.domain.models.UserStorage;
import com.guille.media.reproductor.uploader.storage.domain.ports.ObjectStorageService;
import com.guille.media.reproductor.uploader.storage.domain.ports.StorageEventPublisher;
import com.guille.media.reproductor.uploader.storage.domain.ports.StorageRepository;
import com.guille.media.reproductor.uploader.storage.domain.ports.UserStorageRepository;
import com.guille.media.reproductor.uploader.storage.domain.service.StorageService;
import com.guille.media.reproductor.uploader.storage.domain.service.UploadPolicy;
import com.guille.media.reproductor.uploader.storage.domain.vos.BucketName;
import com.guille.media.reproductor.uploader.storage.domain.vos.StorageKey;
import com.guille.media.reproductor.uploader.storage.domain.vos.StorageLocation;
import com.guille.media.reproductor.uploader.storage.domain.vos.StorageMetadata;

import org.junit.jupiter.api.Test;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Instant;

class AppStorageServiceTest {

  private final ObjectStorageService objectStoragePort = mock(ObjectStorageService.class);
  private final StorageKeyGenerator storageKeyGenerator = new StorageKeyGenerator();
  private final UploadPolicy uploadPolicy = mock(UploadPolicy.class);
  private final StorageRepository storageRepository = mock(StorageRepository.class);
  private final UserServiceCommandPort userServiceQueryPort = mock(UserServiceCommandPort.class);
  private final UserProvider userProvider = mock(UserProvider.class);
  private final UserStorageRepository userStorageRepository = mock(UserStorageRepository.class);
  private final StorageEventPublisher eventPublisher = mock(StorageEventPublisher.class);

  private final StorageService service =
      new AppStorageService(
          objectStoragePort,
          storageKeyGenerator,
          uploadPolicy,
          storageRepository,
          userServiceQueryPort,
          userProvider,
          userStorageRepository,
          eventPublisher);

  private static final AuthenticatedUser PEPE = new AuthenticatedUser("pepe", "pepe@mvflix.dev");

  private static final UserStorage PEPE_STORAGE =
      new UserStorage(
          1L, BucketName.of("movies"), "pepe", StorageQuota.ofGigabytes(10), new StorageUsage(10));

  private static StoreObject completedObject(long storageId, String owner) {
    return new StoreObject(
        owner,
        new StorageKey("k" + storageId),
        new StorageMetadata("video/mp4", 1024, null, Instant.now()),
        Instant.now(),
        storageId,
        StorageSessionStatus.COMPLETED);
  }

  @Test
  void getUserStorageReturnsQuotaOfAuthenticatedUser() {
    when(userProvider.getAuthenticatedUser()).thenReturn(Mono.just(PEPE));
    when(userStorageRepository.findByOwnerUsername("pepe")).thenReturn(Mono.just(PEPE_STORAGE));

    StepVerifier.create(service.getUserStorage())
        .expectNext(PEPE_STORAGE)
        .verifyComplete();
  }

  @Test
  void getUserStorageFailsWhenUserHasNoStorage() {
    when(userProvider.getAuthenticatedUser()).thenReturn(Mono.just(PEPE));
    when(userStorageRepository.findByOwnerUsername("pepe")).thenReturn(Mono.empty());

    StepVerifier.create(service.getUserStorage())
        .expectError(UserStorageNotFoundException.class)
        .verify();
  }

  @Test
  void deleteObjectDeletesBlobReleasesQuotaAndMarksDeletedWhenOwner() {
    StoreObject object = completedObject(7L, "pepe");

    when(userProvider.getAuthenticatedUser()).thenReturn(Mono.just(PEPE));
    when(storageRepository.findById(7L)).thenReturn(Mono.just(object));
    when(userStorageRepository.findByOwnerUsername("pepe")).thenReturn(Mono.just(PEPE_STORAGE));
    when(userStorageRepository.releaseStorage("pepe", 1024)).thenReturn(Mono.just(1L));
    when(storageRepository.markDeleted(7L)).thenReturn(Mono.just(object));

    StepVerifier.create(service.deleteObject(7L)).verifyComplete();

    verify(objectStoragePort)
        .delete(new StorageLocation(BucketName.of("movies"), new StorageKey("k7")));
    verify(userStorageRepository).releaseStorage("pepe", 1024);
    verify(storageRepository).markDeleted(7L);
  }

  @Test
  void deleteObjectRejectsWhenNotOwner() {
    StoreObject object = completedObject(7L, "otra");

    when(userProvider.getAuthenticatedUser()).thenReturn(Mono.just(PEPE));
    when(storageRepository.findById(7L)).thenReturn(Mono.just(object));

    StepVerifier.create(service.deleteObject(7L))
        .expectError(StorageObjectNotAvailable.class)
        .verify();

    verifyNoInteractions(userStorageRepository);
    verifyNoInteractions(objectStoragePort);
  }

  @Test
  void deleteObjectRejectsWhenNotFound() {
    when(userProvider.getAuthenticatedUser()).thenReturn(Mono.just(PEPE));
    when(storageRepository.findById(99L)).thenReturn(Mono.empty());

    StepVerifier.create(service.deleteObject(99L))
        .expectError(StorageObjectNotAvailable.class)
        .verify();
  }
}