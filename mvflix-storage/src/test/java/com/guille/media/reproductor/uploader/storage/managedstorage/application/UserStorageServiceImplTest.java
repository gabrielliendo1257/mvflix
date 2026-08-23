package com.guille.media.reproductor.uploader.storage.managedstorage.application;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.guille.media.reproductor.uploader.storage.shared.security.AuthenticatedUser;
import com.guille.media.reproductor.uploader.storage.shared.security.UserProvider;
import com.guille.media.reproductor.uploader.storage.managedstorage.domain.exception.UserStorageNotFoundException;
import com.guille.media.reproductor.uploader.storage.managedstorage.domain.model.StorageQuota;
import com.guille.media.reproductor.uploader.storage.managedstorage.domain.model.StorageUsage;
import com.guille.media.reproductor.uploader.storage.managedstorage.domain.model.UserStorage;
import com.guille.media.reproductor.uploader.storage.managedstorage.domain.port.ObjectStorageService;
import com.guille.media.reproductor.uploader.storage.managedstorage.domain.port.UserStorageRepository;
import com.guille.media.reproductor.uploader.storage.managedstorage.domain.model.BucketName;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

class UserStorageServiceImplTest {

  private final UserProvider userProvider = mock(UserProvider.class);
  private final UserStorageRepository userStorageRepository = mock(UserStorageRepository.class);
  private final ObjectStorageService objectStoragePort = mock(ObjectStorageService.class);

  private final UserStorageServiceImpl service =
      new UserStorageServiceImpl(userProvider, userStorageRepository, objectStoragePort);

  private static final AuthenticatedUser PEPE = new AuthenticatedUser("pepe", "pepe@mvflix.dev");

  private static final UserStorage PEPE_STORAGE =
      new UserStorage(
          1L, BucketName.of("movies"), "pepe", StorageQuota.ofGigabytes(10), new StorageUsage(10));

  @BeforeEach
  void setUp() {
    ReflectionTestUtils.setField(this.service, "usersBucket", "usuarios");
  }

  @Test
  void getUserStorageReturnsQuotaOfAuthenticatedUser() {
    when(this.userProvider.getAuthenticatedUser()).thenReturn(Mono.just(PEPE));
    when(this.userStorageRepository.findByOwnerUsername("pepe")).thenReturn(Mono.just(PEPE_STORAGE));

    StepVerifier.create(this.service.getUserStorage()).expectNext(PEPE_STORAGE).verifyComplete();
  }

  @Test
  void getUserStorageFailsWhenUserHasNoStorage() {
    when(this.userProvider.getAuthenticatedUser()).thenReturn(Mono.just(PEPE));
    when(this.userStorageRepository.findByOwnerUsername("pepe")).thenReturn(Mono.empty());

    StepVerifier.create(this.service.getUserStorage())
        .expectError(UserStorageNotFoundException.class)
        .verify();
  }

  @Test
  void ensureUserStorageCreatesRowAndLayoutWhenUserDoesNotExist() {
    when(this.userStorageRepository.findByOwnerUsername("pepe")).thenReturn(Mono.empty());
    UserStorage provisioned =
        new UserStorage(
            null, BucketName.of("usuarios"), "pepe", new StorageQuota(2048), new StorageUsage(0));
    when(this.userStorageRepository.save(any(UserStorage.class))).thenReturn(Mono.just(provisioned));
    when(this.objectStoragePort.ensureUserStorageLayout(BucketName.of("usuarios"), "pepe"))
        .thenReturn(Mono.empty());

    StepVerifier.create(this.service.ensureUserStorage("pepe", 2048)).verifyComplete();

    verify(this.userStorageRepository).save(any(UserStorage.class));
    verify(this.objectStoragePort).ensureUserStorageLayout(BucketName.of("usuarios"), "pepe");
  }

  @Test
  void ensureUserStorageSkipsRowCreationWhenUserAlreadyExists() {
    when(this.userStorageRepository.findByOwnerUsername("pepe")).thenReturn(Mono.just(PEPE_STORAGE));
    when(this.objectStoragePort.ensureUserStorageLayout(BucketName.of("movies"), "pepe"))
        .thenReturn(Mono.empty());

    StepVerifier.create(this.service.ensureUserStorage("pepe", 2048)).verifyComplete();

    verify(this.userStorageRepository, never()).save(any(UserStorage.class));
  }
}