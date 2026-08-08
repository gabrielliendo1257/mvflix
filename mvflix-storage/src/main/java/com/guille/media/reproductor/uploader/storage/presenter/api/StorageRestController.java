package com.guille.media.reproductor.uploader.storage.presenter.api;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.guille.media.reproductor.uploader.storage.domain.service.StorageService;
import com.guille.media.reproductor.uploader.storage.presenter.dto.request.ProvisionRequest;
import com.guille.media.reproductor.uploader.storage.presenter.dto.request.StreamingRequest;
import com.guille.media.reproductor.uploader.storage.presenter.dto.request.UploadRequest;
import com.guille.media.reproductor.uploader.storage.presenter.dto.response.QuotaResponse;
import com.guille.media.reproductor.uploader.storage.presenter.dto.response.StreamingSessionResponse;
import com.guille.media.reproductor.uploader.storage.presenter.dto.response.UploadResponse;
import com.guille.media.reproductor.uploader.storage.presenter.mapper.UploadMapper;

import jakarta.validation.Valid;

import reactor.core.publisher.Mono;

@RestController
@RequestMapping(value = "/api/v1/movie/storage", produces = MediaType.APPLICATION_JSON_VALUE)
public class StorageRestController {

  private final StorageService storageService;
  private final UploadMapper uploadMapper;

  public StorageRestController(StorageService storageService, UploadMapper uploadMapper) {
    this.storageService = storageService;
    this.uploadMapper = uploadMapper;
  }

  @PostMapping(
      value = "/upload",
      consumes = MediaType.APPLICATION_JSON_VALUE,
      produces = MediaType.APPLICATION_JSON_VALUE)
  public Mono<ResponseEntity<UploadResponse>> uploadSession(
      @Valid @RequestBody UploadRequest uploadRequest) {
    return this.storageService
        .createUploadSession(this.uploadMapper.toUploadCommand(uploadRequest))
        .map(this.uploadMapper::toUploadResponse)
        .map(ResponseEntity::ok);
  }

  @PostMapping(value = "/upload/{uploadId}/complete")
  public Mono<ResponseEntity<Void>> completeUpload(@PathVariable Long uploadId) {
    return this.storageService.completeUpload(uploadId).thenReturn(ResponseEntity.ok().build());
  }

  @PostMapping(value = "/streaming", consumes = MediaType.APPLICATION_JSON_VALUE)
  public Mono<ResponseEntity<StreamingSessionResponse>> streaming(
      @Valid @RequestBody StreamingRequest streamingRequest) {
    return this.storageService
        .generateStreamingSession(this.uploadMapper.toStreamingCommand(streamingRequest))
        .map(this.uploadMapper::toStreamingSessionResponse)
        .map(ResponseEntity::ok);
  }

  @GetMapping(value = "/quota")
  public Mono<ResponseEntity<QuotaResponse>> quota() {
    return this.storageService
        .getUserStorage()
        .map(
            userStorage ->
                new QuotaResponse(
                    userStorage.getOwnerUsername(),
                    userStorage.getBucketName().bucketName(),
                    userStorage.getStorageQuota().maxBytes(),
                    userStorage.getStorageUsage().getCurrentBytesUsage(),
                    userStorage
                        .getStorageQuota()
                        .remainingBytes(userStorage.getStorageUsage().getCurrentBytesUsage())))
        .map(ResponseEntity::ok);
  }

  @DeleteMapping(value = "/{storageId}")
  public Mono<ResponseEntity<Void>> deleteObject(@PathVariable Long storageId) {
    return this.storageService
        .deleteObject(storageId)
        .thenReturn(ResponseEntity.noContent().build());
  }

  /**
   * Provisiona el espacio del usuario (fila de {@code user_storage} + carpetas
   * en el bucket dedicado). Invocado por el flujo de registro/login.
   */
  @PostMapping(value = "/users/{username}/provision")
  public Mono<ResponseEntity<Void>> provisionUserStorage(
      @PathVariable String username, @RequestBody(required = false) ProvisionRequest request) {
    long quotaBytes = request == null ? 0L : request.quotaBytes();
    return this.storageService
        .ensureUserStorage(username, quotaBytes)
        .thenReturn(ResponseEntity.ok().build());
  }
}
