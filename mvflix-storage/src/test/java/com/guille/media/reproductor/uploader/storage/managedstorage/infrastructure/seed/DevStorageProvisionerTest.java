package com.guille.media.reproductor.uploader.storage.managedstorage.infrastructure.seed;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.guille.mvflix.devseed.DevUser;
import com.guille.media.reproductor.uploader.storage.managedstorage.application.UserStorageService;

import org.junit.jupiter.api.Test;

import reactor.core.publisher.Mono;

class DevStorageProvisionerTest {

  private static final long TEN_GB = 10L * 1024 * 1024 * 1024;

  private final UserStorageService userStorageService = mock(UserStorageService.class);
  private final DevStorageProvisioner provisioner = new DevStorageProvisioner(userStorageService);

  @Test
  void provisionsDevUserWithConfiguredQuota() {
    when(userStorageService.ensureUserStorage(any(), anyLong())).thenReturn(Mono.empty());
    DevUser user = devUser("Javier", TEN_GB);

    provisioner.seed(user);

    verify(userStorageService).ensureUserStorage("Javier", TEN_GB);
  }

  @Test
  void doesNotFailWhenProvisioningFails() {
    when(userStorageService.ensureUserStorage(any(), anyLong()))
        .thenReturn(Mono.error(new IllegalStateException("minio down")));
    DevUser user = devUser("Admin", TEN_GB);

    provisioner.seed(user);

    verify(userStorageService).ensureUserStorage("Admin", TEN_GB);
  }

  private static DevUser devUser(String username, long quotaBytes) {
    DevUser user = new DevUser();
    user.setUsername(username);
    user.setQuotaBytes(quotaBytes);
    return user;
  }
}