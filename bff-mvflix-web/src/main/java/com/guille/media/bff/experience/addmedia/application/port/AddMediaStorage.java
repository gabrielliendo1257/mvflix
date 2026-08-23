package com.guille.media.bff.experience.addmedia.application.port;

import com.guille.media.bff.app.dto.UploadCreateRequest;
import com.guille.media.bff.app.dto.UploadSessionDto;
import com.guille.media.bff.app.dto.UploadStatusDto;

import reactor.core.publisher.Mono;

/**
 * Puerto del contexto Add Media hacia STORAGE. Solo ciclo de vida de la
 * sesión de upload del alta: preparar, consultar, cancelar y compensar.
 * Cuota, streaming y bibliotecas quedan fuera.
 */
public interface AddMediaStorage {

  /** Crea la sesión y devuelve instrucciones para el PUT directo del navegador. */
  Mono<UploadSessionDto> prepareUpload(UploadCreateRequest file);

  /** Estado REAL de la sesión (fuente de verdad del objeto físico). */
  Mono<UploadStatusDto> getUploadState(Long uploadId);

  /** Cancela la sesión (storage libera la cuota reservada). */
  Mono<Void> cancelUpload(Long uploadId);

  /** Compensación: elimina el objeto huérfano (restaura cuota). Idempotente aguas abajo. */
  Mono<Void> deleteObject(Long storageId);
}
