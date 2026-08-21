package com.guille.media.bff.presenter.api;

import com.guille.media.bff.app.dto.IdentifyAssetRequest;
import com.guille.media.bff.app.dto.LibraryDto;
import com.guille.media.bff.app.dto.MediaAssetDto;
import com.guille.media.bff.app.dto.MediaAssetPageDto;
import com.guille.media.bff.app.dto.RegisterLibraryRequest;
import com.guille.media.bff.app.service.WebLibraryService;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
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

  @PostMapping(
      consumes = MediaType.APPLICATION_JSON_VALUE,
      produces = MediaType.APPLICATION_JSON_VALUE)
  public Mono<ResponseEntity<LibraryDto>> register(
      @RequestBody(required = false) RegisterLibraryRequest request) {
    String rootPath = request == null ? null : request.rootPath();
    return this.webLibraryService
        .register(rootPath)
        .map(library -> ResponseEntity.status(HttpStatus.CREATED).body(library));
  }

  @DeleteMapping(value = "/{libraryId}")
  public Mono<ResponseEntity<Void>> delete(@PathVariable Long libraryId) {
    return this.webLibraryService
        .delete(libraryId)
        .thenReturn(ResponseEntity.noContent().build());
  }

  @PostMapping(value = "/{libraryId}/scan", produces = MediaType.APPLICATION_JSON_VALUE)
  public Mono<MediaAssetPageDto> scan(
      @PathVariable Long libraryId,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size) {
    return this.webLibraryService.scan(libraryId, page, size);
  }

  @DeleteMapping(value = "/{libraryId}/scan")
  public Mono<ResponseEntity<Void>> cancelScan(@PathVariable Long libraryId) {
    return this.webLibraryService
        .cancelScan(libraryId)
        .thenReturn(ResponseEntity.noContent().build());
  }

  @GetMapping(value = "/{libraryId}/unidentified", produces = MediaType.APPLICATION_JSON_VALUE)
  public Mono<MediaAssetPageDto> unidentified(
      @PathVariable Long libraryId,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size) {
    return this.webLibraryService.unidentified(libraryId, page, size);
  }

  @PostMapping(
      value = "/assets/{assetId}/identify",
      produces = MediaType.APPLICATION_JSON_VALUE,
      consumes = MediaType.APPLICATION_JSON_VALUE)
  public Mono<ResponseEntity<MediaAssetDto>> identify(
      @PathVariable Long assetId, @RequestBody(required = false) IdentifyAssetRequest request) {
    return this.webLibraryService
        .identify(
            assetId,
            request == null ? null : request.title(),
            request == null ? null : request.tmdbId(),
            request == null ? null : request.kind())
        .map(ResponseEntity::ok);
  }
}
