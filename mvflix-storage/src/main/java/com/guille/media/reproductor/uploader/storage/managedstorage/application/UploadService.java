package com.guille.media.reproductor.uploader.storage.managedstorage.application;

import com.guille.media.reproductor.uploader.storage.managedstorage.application.command.request.CreateUploadCommand;
import com.guille.media.reproductor.uploader.storage.managedstorage.application.command.response.UploadSession;
import com.guille.media.reproductor.uploader.storage.managedstorage.application.command.response.UploadCompletionResult;
import com.guille.media.reproductor.uploader.storage.managedstorage.application.command.response.UploadSummary;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/** Ciclo de vida de la subida: reserva de cuota, URL presigned y cierre del upload. */
public interface UploadService {
  Mono<UploadSession> createUploadSession(CreateUploadCommand command);

  /**
   * Regenera las instrucciones de subida (presigned PUT fresco) para una
   * sesión PENDING propia. Permite que un cliente que perdió la respuesta
   * original (recarga, caída de red) pueda subir sin crear otra sesión ni
   * reservar cuota dos veces.
   */
  Mono<UploadSession> renewInstructions(Long uploadId);

  /** Lista las últimas sesiones de subida del usuario autenticado. */
  Flux<UploadSummary> listUploads(int limit);


  /**
   * Confirma el cierre de una sesión de {@code clientes confirm}). Si el objeto aún
   * no está en el object store devuelve {@code PENDING_VERIFICATION} sin estados
   * destructivos; la reconciliación la hace el webhook (MinIO es la fuente de
   * verdad de que los bytes llegaron).
   */
  Mono<UploadCompletionResult> completeUpload(Long uploadId);

  /**
   * Devuelve el estado actual de la sesión ({@code PENDING}/{@code COMPLETED}/{@code FAILED}/
   * {@code EXPIRED}). Lo consulta el cliente para conocer el resultado de su subida, incluido
   * el error de verificación ({@code FAILED}: el objeto no se completó pero se liberó la cuota).
   */
  Mono<UploadSession> getUploadStatus(Long uploadId);

  /**
   * Reconcilia la remoción de un objeto en el object store ({@code s3:ObjectRemoved}):
   * si la sesión aún está en {@code PENDING}, la marca {@code FAILED} y libera la cuota.
   * No-op si ya se completó o expiró.
   */
  Mono<Void> handleObjectRemoved(String objectKey);

  /**
   * Cancela una sesión de subida explícitamente por el usuario: libera la cuota
   * reservada y la marca {@code FAILED} con motivo de cancelación.
   *
   * <p>No-op si la sesión ya no está en {@code PENDING} (por ejemplo, el objeto
   * llegó y el webhook la completó). El object store queda a cargo de lo que
   * haga con los bytes parciales.
   */
  Mono<Void> cancelUpload(Long uploadId);

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