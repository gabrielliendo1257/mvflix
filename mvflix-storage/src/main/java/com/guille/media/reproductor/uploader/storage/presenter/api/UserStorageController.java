package com.guille.media.reproductor.uploader.storage.presenter.api;

import com.guille.media.reproductor.uploader.storage.domain.service.UserStorageService;
import com.guille.media.reproductor.uploader.storage.presenter.dto.request.ProvisionRequest;
import com.guille.media.reproductor.uploader.storage.presenter.dto.response.QuotaResponse;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import reactor.core.publisher.Mono;

@RestController
@RequestMapping(value = "/api/v1/movie/storage", produces = MediaType.APPLICATION_JSON_VALUE)
public class UserStorageController {

  private final UserStorageService userStorageService;

  public UserStorageController(UserStorageService userStorageService) {
    this.userStorageService = userStorageService;
  }

  @GetMapping(value = "/quota")
  public Mono<ResponseEntity<QuotaResponse>> quota() {
    return this.userStorageService
        .getUserStorage()
        .map(this::toQuotaResponse)
        .map(ResponseEntity::ok);
  }

  /**
   * Uso real de un usuario concreto (contrato M2M con user-service para
   * decisiones de plan). Devuelve los bytes realmente consumidos.
   */
  @GetMapping(value = "/users/{username}/quota")
  public Mono<ResponseEntity<QuotaResponse>> quotaByUsername(@PathVariable String username) {
    return this.userStorageService
        .getUserStorageBy(username)
        .map(this::toQuotaResponse)
        .map(ResponseEntity::ok);
  }

  private QuotaResponse toQuotaResponse(com.guille.media.reproductor.uploader.storage.domain.models.UserStorage userStorage) {
    return new QuotaResponse(
        userStorage.getOwnerUsername(),
        userStorage.getBucketName().bucketName(),
        userStorage.getStorageQuota().maxBytes(),
        userStorage.getStorageUsage().getCurrentBytesUsage(),
        userStorage
            .getStorageQuota()
            .remainingBytes(userStorage.getStorageUsage().getCurrentBytesUsage()));
  }

  /**
   * Provisiona el espacio del usuario (fila de {@code user_storage} + carpetas
   * en el bucket dedicado). Invocado por el flujo de registro/login.
   */
  @PostMapping(value = "/users/{username}/provision")
  public Mono<ResponseEntity<Void>> provisionUserStorage(
      @PathVariable String username, @RequestBody(required = false) ProvisionRequest request) {
    long quotaBytes = request == null ? 0L : request.quotaBytes();
    return this.userStorageService
        .ensureUserStorage(username, quotaBytes)
        .thenReturn(ResponseEntity.ok().build());
  }
}