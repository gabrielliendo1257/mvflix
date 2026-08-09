package com.guille.media.bff.presenter.api;

import com.guille.media.bff.app.dto.UploadCreateRequest;
import com.guille.media.bff.app.dto.UploadListItem;
import com.guille.media.bff.app.dto.UploadSessionDto;
import com.guille.media.bff.app.dto.UploadStatusDto;
import com.guille.media.bff.app.service.WebUploadsService;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/web/uploads")
public class WebUploadsController {

  private final WebUploadsService webUploadsService;

  public WebUploadsController(WebUploadsService webUploadsService) {
    this.webUploadsService = webUploadsService;
  }

  @GetMapping
  public Flux<UploadListItem> list(
      @RequestHeader(value = "Authorization", required = false) String bearer,
      @RequestParam(defaultValue = "20") int limit) {
    return this.webUploadsService.list(bearer, limit);
  }

  @PostMapping
  public Mono<ResponseEntity<UploadSessionDto>> create(
      @RequestHeader(value = "Authorization", required = false) String bearer,
      @RequestBody UploadCreateRequest request) {
    return this.webUploadsService
        .create(bearer, request)
        .map(ResponseEntity::ok);
  }

  @GetMapping("/{uploadId}")
  public Mono<ResponseEntity<UploadStatusDto>> status(
      @RequestHeader(value = "Authorization", required = false) String bearer,
      @PathVariable Long uploadId) {
    return this.webUploadsService.status(bearer, uploadId).map(ResponseEntity::ok);
  }

  @PostMapping("/{uploadId}/cancel")
  public Mono<ResponseEntity<Void>> cancel(
      @RequestHeader(value = "Authorization", required = false) String bearer,
      @PathVariable Long uploadId) {
    return this.webUploadsService.cancel(bearer, uploadId).thenReturn(ResponseEntity.ok().build());
  }

  @PostMapping("/{uploadId}/complete")
  public Mono<ResponseEntity<Void>> complete(
      @RequestHeader(value = "Authorization", required = false) String bearer,
      @PathVariable Long uploadId) {
    return this.webUploadsService
        .complete(bearer, uploadId)
        .map(status -> ResponseEntity.status(status).build());
  }
}