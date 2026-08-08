package com.guille.media.reproductor.uploader.storage.presenter.api;

import com.guille.media.reproductor.uploader.storage.domain.service.StreamingService;
import com.guille.media.reproductor.uploader.storage.presenter.dto.request.StreamingRequest;
import com.guille.media.reproductor.uploader.storage.presenter.dto.response.StreamingSessionResponse;
import com.guille.media.reproductor.uploader.storage.presenter.mapper.UploadMapper;

import jakarta.validation.Valid;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import reactor.core.publisher.Mono;

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
}