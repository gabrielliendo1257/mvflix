package com.guille.media.reproductor.uploader.storage.infrastructure.seed;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.guille.media.reproductor.uploader.storage.domain.service.UserStorageService;

import org.junit.jupiter.api.Test;

import reactor.core.publisher.Mono;

class DevStorageProvisionerTest {

  private static final long TEN_GB = 10L * 1024 * 1024 * 1024;

  private final UserStorageService userStorageService = mock(UserStorageService.class);
  private final DevStorageProvisioner provisioner = new DevStorageProvisioner(userStorageService);

  @Test
  void provisionsDevUsersWithDefaultQuota() {
    when(userStorageService.ensureUserStorage(any(), anyLong())).thenReturn(Mono.empty());

    provisioner.run(null);

    verify(userStorageService).ensureUserStorage("Javier", TEN_GB);
    verify(userStorageService).ensureUserStorage("Admin", TEN_GB);
  }

  @Test
  void doesNotAbortBootWhenProvisioningFails() {
    when(userStorageService.ensureUserStorage(eq("Javier"), anyLong()))
        .thenReturn(Mono.error(new IllegalStateException("minio down")));
    when(userStorageService.ensureUserStorage(eq("Admin"), anyLong())).thenReturn(Mono.empty());

    provisioner.run(null);

    verify(userStorageService).ensureUserStorage("Admin", TEN_GB);
  }
}