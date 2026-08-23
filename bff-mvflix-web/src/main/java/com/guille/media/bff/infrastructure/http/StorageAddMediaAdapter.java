package com.guille.media.bff.infrastructure.http;

import com.guille.media.bff.app.dto.UploadCreateRequest;
import com.guille.media.bff.app.dto.UploadSessionDto;
import com.guille.media.bff.app.dto.UploadStatusDto;
import com.guille.media.bff.app.ports.StorageWebClient;
import com.guille.media.bff.experience.addmedia.application.port.AddMediaStorage;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Component;

import reactor.core.publisher.Mono;

/** Adapter HTTP del contexto Add Media hacia storage-service. Delega en el cliente existente. */
@Component
@RequiredArgsConstructor
public class StorageAddMediaAdapter implements AddMediaStorage {

  private final StorageWebClient delegate;

  @Override
  public Mono<UploadSessionDto> prepareUpload(UploadCreateRequest file) {
    return this.delegate.createUpload(file);
  }

  @Override
  public Mono<UploadStatusDto> getUploadState(Long uploadId) {
    return this.delegate.uploadStatus(uploadId);
  }

  @Override
  public Mono<Void> cancelUpload(Long uploadId) {
    return this.delegate.cancelUpload(uploadId);
  }

  @Override
  public Mono<Void> deleteObject(Long storageId) {
    return this.delegate.deleteObject(storageId);
  }
}
