package com.guille.media.reproductor.uploader.storage.managedstorage.infrastructure.web;

import com.guille.media.reproductor.uploader.storage.managedstorage.application.command.response.UploadCompletionResult;
import com.guille.media.reproductor.uploader.storage.managedstorage.application.UploadService;
import com.guille.media.reproductor.uploader.storage.managedstorage.infrastructure.web.UploadRequest;
import com.guille.media.reproductor.uploader.storage.managedstorage.infrastructure.web.UploadResponse;
import com.guille.media.reproductor.uploader.storage.managedstorage.infrastructure.web.UploadSummaryResponse;
import com.guille.media.reproductor.uploader.storage.managedstorage.infrastructure.web.UploadMapper;

import jakarta.validation.Valid;

import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Tag(name = "Uploads", description = "Sesiones de upload con cuota: creación, estado, renovación de instrucciones, cancel, complete")
@RestController
@RequestMapping(value = "/api/v1/movie/storage", produces = MediaType.APPLICATION_JSON_VALUE)
public class UploadController {

  private final UploadService uploadService;
  private final UploadMapper uploadMapper;

  public UploadController(UploadService uploadService, UploadMapper uploadMapper) {
    this.uploadService = uploadService;
    this.uploadMapper = uploadMapper;
  }

  @GetMapping(value = "/uploads")
  public Flux<UploadSummaryResponse> uploads(
      @RequestParam(defaultValue = "20") int limit) {
    return this.uploadService.listUploads(limit).map(this.uploadMapper::toUploadSummaryResponse);
  }

  @Operation(summary = "Crea la sesión y reserva cuota atómicamente")
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

  @Operation(summary = "Recupera una sesión por idempotency key del usuario autenticado")
  @GetMapping(value = "/uploads/by-idempotency/{idempotencyKey}")
  public Mono<ResponseEntity<UploadResponse>> uploadByIdempotencyKey(
      @PathVariable String idempotencyKey) {
    return this.uploadService.findUploadByIdempotencyKey(idempotencyKey)
        .map(this.uploadMapper::toUploadResponse)
        .map(ResponseEntity::ok)
        .defaultIfEmpty(ResponseEntity.notFound().build());
  }

  /** Regenera instrucciones de subida para una sesión PENDING propia. */
  @Operation(summary = "Regenera instrucciones presigned para una sesión PENDING propia")
  @PostMapping(
      value = "/upload/{uploadId}/instructions",
      produces = MediaType.APPLICATION_JSON_VALUE)
  public Mono<ResponseEntity<UploadResponse>> renewInstructions(
      @PathVariable Long uploadId) {
    return this.uploadService
        .renewInstructions(uploadId)
        .map(this.uploadMapper::toUploadResponse)
        .map(ResponseEntity::ok);
  }

  @PostMapping(value = "/upload/{uploadId}/cancel")
  public Mono<ResponseEntity<Void>> cancelUpload(@PathVariable Long uploadId) {
    return this.uploadService.cancelUpload(uploadId).thenReturn(ResponseEntity.ok().build());
  }

  @Operation(summary = "Verifica contra MinIO y reconcilia el webhook (202 si aún no llegó)")
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
