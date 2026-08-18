package com.guille.media.bff.presenter.api;

import com.guille.media.bff.app.dto.IdentifyAssetRequest;
import com.guille.media.bff.app.dto.LibraryDto;
import com.guille.media.bff.app.dto.MediaAssetDto;
import com.guille.media.bff.app.service.WebLibraryService;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/** Media server (bibliotecas del operador): scan, listado e identificación. */
@RestController
@RequestMapping("/web/libraries")
public class WebLibrariesController {

  private final WebLibraryService webLibraryService;

  public WebLibrariesController(WebLibraryService webLibraryService) {
    this.webLibraryService = webLibraryService;
  }

  @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
  public Flux<LibraryDto> libraries() {
    return this.webLibraryService.libraries();
  }

  @PostMapping(value = "/{libraryId}/scan", produces = MediaType.APPLICATION_JSON_VALUE)
  public Flux<MediaAssetDto> scan(@PathVariable Long libraryId) {
    return this.webLibraryService.scan(libraryId);
  }

  @GetMapping(value = "/{libraryId}/unidentified", produces = MediaType.APPLICATION_JSON_VALUE)
  public Flux<MediaAssetDto> unidentified(@PathVariable Long libraryId) {
    return this.webLibraryService.unidentified(libraryId);
  }

  @PostMapping(
      value = "/assets/{assetId}/identify",
      produces = MediaType.APPLICATION_JSON_VALUE,
      consumes = MediaType.APPLICATION_JSON_VALUE)
  public Mono<ResponseEntity<MediaAssetDto>> identify(
      @PathVariable Long assetId, @RequestBody(required = false) IdentifyAssetRequest request) {
    return this.webLibraryService
        .identify(assetId, request == null ? null : request.title())
        .map(ResponseEntity::ok);
  }
}
