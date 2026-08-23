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
    return this.translate(this.delegate.createUpload(file));
  }

  @Override
  public Mono<Void> requestCompletion(Long uploadId) {
    return this.translate(this.delegate.completeUpload(uploadId)).then();
  }

  @Override
  public Mono<UploadStatusDto> getUploadState(Long uploadId) {
    return this.translate(this.delegate.uploadStatus(uploadId));
  }

  @Override
  public Mono<UploadSessionDto> refreshInstructions(Long uploadId) {
    return this.translate(this.delegate.renewInstructions(uploadId));
  }

  @Override
  public Mono<Void> cancelUpload(Long uploadId) {
    return this.translate(this.delegate.cancelUpload(uploadId));
  }

  @Override
  public Mono<Void> deleteObject(Long storageId) {
    return this.translate(this.delegate.deleteObject(storageId));
  }

  /**
   * Frontera de traducción: los errores HTTP/WebClient no cruzan hacia la
   * aplicación. 5xx y fallos de conexión son CAÍDAS reintentables; los 4xx se
   * preservan como rechazo con status para decisiones (404/409).
   */
  private <T> reactor.core.publisher.Mono<T> translate(
      reactor.core.publisher.Mono<T> call) {
    return call.onErrorResume(
        org.springframework.web.reactive.function.client.WebClientResponseException.class,
        ex -> {
          if (ex.getStatusCode().is5xxServerError()) {
            return Mono.error(new com.guille.media.bff.experience.addmedia.application.
                DownstreamUnavailableException(ex.getStatusCode().value(),
                    "DOWNSTREAM_UNAVAILABLE", ex.getMessage()));
          }
          return Mono.error(new com.guille.media.bff.experience.addmedia.application.
              DownstreamRejectionException(ex.getStatusCode().value(), ex.getMessage()));
        })
        .onErrorResume(
            org.springframework.web.reactive.function.client.WebClientRequestException.class,
            ex -> Mono.error(new com.guille.media.bff.experience.addmedia.application.
                DownstreamUnavailableException(503,
                    "DOWNSTREAM_UNREACHABLE", ex.getMessage())));
  }
}
