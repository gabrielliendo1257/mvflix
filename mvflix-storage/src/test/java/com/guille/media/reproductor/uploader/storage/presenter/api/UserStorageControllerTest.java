package com.guille.media.reproductor.uploader.storage.presenter.api;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.guille.media.reproductor.uploader.advisors.GlobalExceptionHandler;
import com.guille.media.reproductor.uploader.storage.domain.models.StorageQuota;
import com.guille.media.reproductor.uploader.storage.domain.models.StorageUsage;
import com.guille.media.reproductor.uploader.storage.domain.models.UserStorage;
import com.guille.media.reproductor.uploader.storage.domain.service.UserStorageService;
import com.guille.media.reproductor.uploader.storage.domain.vos.BucketName;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

import reactor.core.publisher.Mono;

class UserStorageControllerTest {

  private static final String BASE = "/api/v1/movie/storage";

  private final UserStorageService userStorageService = mock(UserStorageService.class);
  private final UserStorageController controller = new UserStorageController(userStorageService);

  private final WebTestClient client =
      WebTestClient.bindToController(controller)
          .controllerAdvice(new GlobalExceptionHandler())
          .build();

  @Test
  void quotaEndpointReturnsQuotaOfAuthenticatedUser() {
    UserStorage userStorage =
        new UserStorage(
            1L,
            BucketName.of("movies"),
            "pepe",
            StorageQuota.ofGigabytes(10),
            new StorageUsage(1024));
    when(this.userStorageService.getUserStorage()).thenReturn(Mono.just(userStorage));

    long quotaBytes = 10L * 1024 * 1024 * 1024;

    client
        .get()
        .uri(BASE + "/quota")
        .exchange()
        .expectStatus()
        .isOk()
        .expectBody()
        .jsonPath("$.ownerUsername")
        .isEqualTo("pepe")
        .jsonPath("$.bucketName")
        .isEqualTo("movies")
        .jsonPath("$.quotaBytes")
        .isEqualTo(quotaBytes)
        .jsonPath("$.usedBytes")
        .isEqualTo(1024)
        .jsonPath("$.remainingBytes")
        .isEqualTo(quotaBytes - 1024);
  }

  @Test
  void provisionUserStorageCallsEnsureAndReturns200() {
    when(this.userStorageService.ensureUserStorage("pepe", 2048L)).thenReturn(Mono.empty());

    client
        .post()
        .uri(BASE + "/users/pepe/provision")
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue("{\"quota_bytes\":2048}")
        .exchange()
        .expectStatus()
        .isOk();
  }

  @Test
  void provisionUserStorageWithDefaultQuotaWhenNoBody() {
    when(this.userStorageService.ensureUserStorage("pepe", 0L)).thenReturn(Mono.empty());

    client.post().uri(BASE + "/users/pepe/provision").exchange().expectStatus().isOk();
  }

  @Test
  void quotaByUsernameReturnsRealUsageForM2m() {
    UserStorage userStorage =
        new UserStorage(
            1L,
            BucketName.of("movies"),
            "pepe",
            StorageQuota.ofGigabytes(10),
            new StorageUsage(2048));
    when(this.userStorageService.getUserStorageBy("pepe")).thenReturn(Mono.just(userStorage));

    client
        .get()
        .uri(BASE + "/users/pepe/quota")
        .exchange()
        .expectStatus()
        .isOk()
        .expectBody()
        .jsonPath("$.ownerUsername")
        .isEqualTo("pepe")
        .jsonPath("$.usedBytes")
        .isEqualTo(2048);
  }
}