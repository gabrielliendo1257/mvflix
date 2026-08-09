package com.guille.media.bff.infrastructure.http;

import com.guille.media.bff.app.dto.QuotaSnapshot;
import com.guille.media.bff.app.dto.UploadCreateRequest;
import com.guille.media.bff.app.dto.UploadListItem;
import com.guille.media.bff.app.dto.UploadSessionDto;
import com.guille.media.bff.app.dto.UploadStatusDto;
import com.guille.media.bff.app.ports.StorageWebClient;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
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
  public Mono<QuotaSnapshot> quota(String bearer) {
    return this.get(bearer, API + "/quota", QuotaSnapshot.class);
  }

  @Override
  public Flux<UploadListItem> listUploads(String bearer, int limit) {
    return this.storageWebClient
        .get()
        .uri(uriBuilder -> uriBuilder.path(API + "/uploads").queryParam("limit", limit).build())
        .header(HttpHeaders.AUTHORIZATION, bearer)
        .retrieve()
        .bodyToFlux(UploadListItem.class);
  }

  @Override
  public Mono<UploadSessionDto> createUpload(String bearer, UploadCreateRequest request) {
    return this.storageWebClient
        .post()
        .uri(API + "/upload")
        .header(HttpHeaders.AUTHORIZATION, bearer)
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(request)
        .retrieve()
        .bodyToMono(UploadSessionDto.class);
  }

  @Override
  public Mono<UploadStatusDto> uploadStatus(String bearer, Long uploadId) {
    return this.get(bearer, API + "/upload/" + uploadId, UploadStatusDto.class);
  }

  @Override
  public Mono<Void> cancelUpload(String bearer, Long uploadId) {
    return this.postAndDiscard(bearer, API + "/upload/" + uploadId + "/cancel");
  }

  @Override
  public Mono<HttpStatus> completeUpload(String bearer, Long uploadId) {
    return this.storageWebClient
        .post()
        .uri(API + "/upload/" + uploadId + "/complete")
        .header(HttpHeaders.AUTHORIZATION, bearer)
        .retrieve()
        .toBodilessEntity()
        .map(entity -> (HttpStatus) entity.getStatusCode());
  }

  private <T> Mono<T> get(String bearer, String uri, Class<T> type) {
    return this.storageWebClient
        .get()
        .uri(uri)
        .header(HttpHeaders.AUTHORIZATION, bearer)
        .retrieve()
        .bodyToMono(type);
  }

  private Mono<Void> postAndDiscard(String bearer, String uri) {
    return this.storageWebClient
        .post()
        .uri(uri)
        .header(HttpHeaders.AUTHORIZATION, bearer)
        .retrieve()
        .toBodilessEntity()
        .then();
  }
}