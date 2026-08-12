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

  Mono<QuotaSnapshot> quota();

  Flux<UploadListItem> listUploads(int limit);

  Mono<UploadSessionDto> createUpload(UploadCreateRequest request);

  Mono<UploadStatusDto> uploadStatus(Long uploadId);

  Mono<Void> cancelUpload(Long uploadId);

  Mono<HttpStatus> completeUpload(Long uploadId);

  /** Rollback: borra el objeto en el object store y restaura la cuota del usuario. */
  Mono<Void> deleteObject(Long storageId);
}