package com.guille.media.bff.infrastructure.http;

import com.guille.media.bff.app.dto.DiscoveredFileDto;
import com.guille.media.bff.app.dto.LibraryDto;
import com.guille.media.bff.app.dto.QuotaSnapshot;
import com.guille.media.bff.app.dto.StreamingRequest;
import com.guille.media.bff.app.dto.StreamingSessionDto;
import com.guille.media.bff.app.dto.UploadCreateRequest;
import com.guille.media.bff.app.dto.UploadListItem;
import com.guille.media.bff.app.dto.UploadSessionDto;
import com.guille.media.bff.app.dto.UploadStatusDto;
import com.guille.media.bff.app.ports.StorageWebClient;

import lombok.RequiredArgsConstructor;

import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
public class StorageWebClientAdapter implements StorageWebClient {

  private static final String API = "/api/v1/movie/storage";

  private final WebClient storageWebClient;

  @Override
  public Mono<QuotaSnapshot> quota() {
    return this.get(API + "/quota", QuotaSnapshot.class);
  }

  @Override
  public Flux<UploadListItem> listUploads(int limit) {
    return this.storageWebClient
        .get()
        .uri(uriBuilder -> uriBuilder.path(API + "/uploads").queryParam("limit", limit).build())
                .retrieve()
        .bodyToFlux(UploadListItem.class);
  }

  @Override
  public Mono<UploadSessionDto> createUpload(UploadCreateRequest request) {
    return this.storageWebClient
        .post()
        .uri(API + "/upload")
                .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(request)
        .retrieve()
        .bodyToMono(UploadSessionDto.class);
  }

  @Override
  public Mono<UploadStatusDto> uploadStatus(Long uploadId) {
    return this.get(API + "/upload/" + uploadId, UploadStatusDto.class);
  }

  @Override
  public Mono<Void> cancelUpload(Long uploadId) {
    return this.postAndDiscard(API + "/upload/" + uploadId + "/cancel");
  }

  @Override
  public Mono<HttpStatus> completeUpload(Long uploadId) {
    return this.storageWebClient
        .post()
        .uri(API + "/upload/" + uploadId + "/complete")
                .retrieve()
        .toBodilessEntity()
        .map(entity -> (HttpStatus) entity.getStatusCode());
  }

  @Override
  public Mono<Void> deleteObject(Long storageId) {
    return this.storageWebClient
        .delete()
        .uri(API + "/" + storageId)
                .retrieve()
        .toBodilessEntity()
        .then();
  }

  @Override
  public Mono<StreamingSessionDto> stream(String objectId) {
    return this.storageWebClient
        .post()
        .uri(API + "/streaming")
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(new StreamingRequest(objectId))
        .retrieve()
        .bodyToMono(StreamingSessionDto.class);
  }

  @Override
  public Flux<LibraryDto> listLibraries() {
    return this.storageWebClient
        .get()
        .uri(API + "/libraries")
        .retrieve()
        .bodyToFlux(LibraryDto.class);
  }

  @Override
  public Mono<LibraryDto> createLibrary(String rootPath) {
    return this.storageWebClient
        .post()
        .uri(API + "/libraries")
        .bodyValue(new RegisterLibraryBody(rootPath))
        .retrieve()
        .bodyToMono(LibraryDto.class);
  }

  @Override
  public Mono<Void> deleteLibrary(Long libraryId) {
    return this.storageWebClient
        .delete()
        .uri(API + "/libraries/" + libraryId)
        .retrieve()
        .toBodilessEntity()
        .then();
  }

  private record RegisterLibraryBody(String rootPath) {}

  @Override
  public Flux<DiscoveredFileDto> listLibraryFiles(Long libraryId) {
    return this.storageWebClient
        .get()
        .uri(API + "/libraries/" + libraryId + "/files")
        .retrieve()
        .bodyToFlux(DiscoveredFileDto.class);
  }

  @Override
  public Mono<ResponseEntity<Flux<DataBuffer>>> streamLibraryFile(
      Long libraryId, String relativePath, String rangeHeader) {
    var spec =
        this.storageWebClient
            .get()
            .uri(uriBuilder -> uriBuilder
                .path(API + "/libraries/" + libraryId + "/files")
                .path("/" + relativePath)
                .build());
    if (rangeHeader != null && !rangeHeader.isBlank()) {
      spec = spec.header(HttpHeaders.RANGE, rangeHeader);
    }
    return spec
        .retrieve()
        .toEntityFlux(DataBuffer.class)
        .onErrorResume(
            org.springframework.web.reactive.function.client.WebClientResponseException.class,
            ex -> {
              HttpHeaders headers = new HttpHeaders();
              headers.set(HttpHeaders.ACCEPT_RANGES, "bytes");
              String contentRange = ex.getHeaders().getFirst(HttpHeaders.CONTENT_RANGE);
              if (contentRange != null) {
                headers.set(HttpHeaders.CONTENT_RANGE, contentRange);
              }
              return Mono.just(ResponseEntity.status(ex.getStatusCode()).headers(headers).build());
            });
  }

  private <T> Mono<T> get(String uri, Class<T> type) {
    return this.storageWebClient
        .get()
        .uri(uri)
                .retrieve()
        .bodyToMono(type);
  }

  private Mono<Void> postAndDiscard(String uri) {
    return this.storageWebClient
        .post()
        .uri(uri)
                .retrieve()
        .toBodilessEntity()
        .then();
  }
}