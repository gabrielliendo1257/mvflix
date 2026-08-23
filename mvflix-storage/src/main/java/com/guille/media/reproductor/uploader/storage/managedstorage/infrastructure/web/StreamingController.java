package com.guille.media.reproductor.uploader.storage.managedstorage.infrastructure.web;

import com.guille.media.reproductor.uploader.storage.managedstorage.application.StreamingService;
import com.guille.media.reproductor.uploader.storage.managedstorage.infrastructure.web.StreamingRequest;
import com.guille.media.reproductor.uploader.storage.managedstorage.infrastructure.web.StreamingSessionResponse;
import com.guille.media.reproductor.uploader.storage.managedstorage.infrastructure.web.UploadMapper;

import jakarta.validation.Valid;

import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import reactor.core.publisher.Mono;

@Tag(name = "Streaming", description = "URLs presignadas de reproducción (preview del dueño y M2M catálogo)")
@RestController
@RequestMapping(value = "/api/v1/movie/storage", produces = MediaType.APPLICATION_JSON_VALUE)
public class StreamingController {

  private final StreamingService streamingService;
  private final UploadMapper uploadMapper;

  public StreamingController(StreamingService streamingService, UploadMapper uploadMapper) {
    this.streamingService = streamingService;
    this.uploadMapper = uploadMapper;
  }

  @PostMapping(value = "/streaming", consumes = MediaType.APPLICATION_JSON_VALUE)
  public Mono<ResponseEntity<StreamingSessionResponse>> streaming(
      @Valid @RequestBody StreamingRequest streamingRequest) {
    return this.streamingService
        .generateStreamingSession(this.uploadMapper.toStreamingCommand(streamingRequest))
        .map(this.uploadMapper::toStreamingSessionResponse)
        .map(ResponseEntity::ok);
  }

  /**
   * Playback M2M del catálogo: requiere scope {@code storage.stream}. El
   * servicio invocante (movies/BFF) ya validó visibilidad; storage solo
   * comprueba disponibilidad del objeto y emite la URL presignada.
   */
  @PostMapping(value = "/catalog/streaming", consumes = MediaType.APPLICATION_JSON_VALUE)
  public Mono<ResponseEntity<StreamingSessionResponse>> catalogStreaming(
      @Valid @RequestBody StreamingRequest streamingRequest) {
    return this.streamingService
        .generateCatalogStreamingSession(this.uploadMapper.toStreamingCommand(streamingRequest))
        .map(this.uploadMapper::toStreamingSessionResponse)
        .map(ResponseEntity::ok);
  }
}