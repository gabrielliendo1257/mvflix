package com.guille.media.bff.app.service;

import com.guille.media.bff.app.dto.UploadCreateRequest;
import com.guille.media.bff.app.dto.UploadListItem;
import com.guille.media.bff.app.dto.UploadSessionDto;
import com.guille.media.bff.app.dto.UploadStatusDto;
import com.guille.media.bff.app.ports.StorageWebClient;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class WebUploadsService {

  private final StorageWebClient storageWebClient;

  public Flux<UploadListItem> list(String bearerToken, int limit) {
    return this.storageWebClient.listUploads(bearerToken, limit);
  }

  public Mono<UploadSessionDto> create(String bearerToken, UploadCreateRequest request) {
    return this.storageWebClient.createUpload(bearerToken, request);
  }

  public Mono<UploadStatusDto> status(String bearerToken, Long uploadId) {
    return this.storageWebClient.uploadStatus(bearerToken, uploadId);
  }

  public Mono<Void> cancel(String bearerToken, Long uploadId) {
    return this.storageWebClient.cancelUpload(bearerToken, uploadId);
  }

  public Mono<HttpStatus> complete(String bearerToken, Long uploadId) {
    return this.storageWebClient.completeUpload(bearerToken, uploadId);
  }
}