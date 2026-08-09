package com.guille.media.bff.app.ports;

import com.guille.media.bff.app.dto.QuotaSnapshot;
import com.guille.media.bff.app.dto.UploadCreateRequest;
import com.guille.media.bff.app.dto.UploadListItem;
import com.guille.media.bff.app.dto.UploadSessionDto;
import com.guille.media.bff.app.dto.UploadStatusDto;

import org.springframework.http.HttpStatus;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/** Contrato hacia mvflix-storage (sesiones de subida y cuota del usuario). */
public interface StorageWebClient {

  Mono<QuotaSnapshot> quota(String bearer);

  Flux<UploadListItem> listUploads(String bearer, int limit);

  Mono<UploadSessionDto> createUpload(String bearer, UploadCreateRequest request);

  Mono<UploadStatusDto> uploadStatus(String bearer, Long uploadId);

  Mono<Void> cancelUpload(String bearer, Long uploadId);

  Mono<HttpStatus> completeUpload(String bearer, Long uploadId);
}