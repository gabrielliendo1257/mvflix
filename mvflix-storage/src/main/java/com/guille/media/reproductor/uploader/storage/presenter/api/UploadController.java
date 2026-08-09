package com.guille.media.reproductor.uploader.storage.presenter.api;

import com.guille.media.reproductor.uploader.storage.app.commands.response.UploadCompletionResult;
import com.guille.media.reproductor.uploader.storage.domain.service.UploadService;
import com.guille.media.reproductor.uploader.storage.presenter.dto.request.UploadRequest;
import com.guille.media.reproductor.uploader.storage.presenter.dto.response.UploadResponse;
import com.guille.media.reproductor.uploader.storage.presenter.mapper.UploadMapper;

import jakarta.validation.Valid;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import reactor.core.publisher.Mono;

@RestController
@RequestMapping(value = "/api/v1/movie/storage", produces = MediaType.APPLICATION_JSON_VALUE)
public class UploadController {

  private final UploadService uploadService;
  private final UploadMapper uploadMapper;

  public UploadController(UploadService uploadService, UploadMapper uploadMapper) {
    this.uploadService = uploadService;
    this.uploadMapper = uploadMapper;
  }

  @PostMapping(
      value = "/upload",
      consumes = MediaType.APPLICATION_JSON_VALUE,
      produces = MediaType.APPLICATION_JSON_VALUE)
  public Mono<ResponseEntity<UploadResponse>> uploadSession(
      @Valid @RequestBody UploadRequest uploadRequest) {
    return this.uploadService
        .createUploadSession(this.uploadMapper.toUploadCommand(uploadRequest))
        .map(this.uploadMapper::toUploadResponse)
        .map(ResponseEntity::ok);
  }

  @GetMapping(value = "/upload/{uploadId}")
  public Mono<ResponseEntity<UploadResponse>> uploadStatus(@PathVariable Long uploadId) {
    return this.uploadService
        .getUploadStatus(uploadId)
        .map(this.uploadMapper::toUploadResponse)
        .map(ResponseEntity::ok);
  }

  @PostMapping(value = "/upload/{uploadId}/cancel")
  public Mono<ResponseEntity<Void>> cancelUpload(@PathVariable Long uploadId) {
    return this.uploadService.cancelUpload(uploadId).thenReturn(ResponseEntity.ok().build());
  }

  @PostMapping(value = "/upload/{uploadId}/complete")
  public Mono<ResponseEntity<Void>> completeUpload(@PathVariable Long uploadId) {
    return this.uploadService
        .completeUpload(uploadId)
        .map(
            result ->
                result.status() == UploadCompletionResult.UploadCompletionStatus.PENDING_VERIFICATION
                    ? ResponseEntity.accepted().build()
                    : ResponseEntity.ok().build());
  }
}