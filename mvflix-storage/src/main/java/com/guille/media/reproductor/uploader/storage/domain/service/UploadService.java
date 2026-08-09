package com.guille.media.reproductor.uploader.storage.domain.service;

import com.guille.media.reproductor.uploader.storage.app.commands.requests.CreateUploadCommand;
import com.guille.media.reproductor.uploader.storage.app.commands.response.UploadSession;

import reactor.core.publisher.Mono;

/** Ciclo de vida de la subida: reserva de cuota, URL presigned y cierre del upload. */
public interface UploadService {
  Mono<UploadSession> createUploadSession(CreateUploadCommand command);

  Mono<Void> completeUpload(Long uploadId);

  /**
   * Completa un upload identificado por la clave del objeto en el object store.
   *
   * <p>Camino de reconciliación: lo invoca el webhook de eventos de MinIO ({@code
   * s3:ObjectCreated}). El object store es la fuente de verdad de que los bytes llegaron; la
   * transición a {@code COMPLETED} solo ocurre la primera vez y el evento de dominio se publica
   * únicamente entonces (idempotente frente a la confirmación del cliente).
   */
  Mono<Void> completeUploadByKey(String objectKey);
}