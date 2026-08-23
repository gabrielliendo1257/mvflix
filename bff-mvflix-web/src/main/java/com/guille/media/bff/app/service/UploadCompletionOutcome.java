package com.guille.media.bff.app.service;

import com.guille.media.bff.app.dto.MovieDto;

/**
 * Resultado del cierre del alta. Sellado: el controller traduce cada variante
 * a su código HTTP sin adivinar por excepciones.
 */
public sealed interface UploadCompletionOutcome {

  /** El objeto llegó, pasó la verificación y Movies persistió la película READY. */
  record Completed(MovieDto movie) implements UploadCompletionOutcome {}

  /**
   * El upload sigue PENDING en storage: puede ser simple demora del webhook o
   * verificación asíncrona. NO es un fallo: no hay rollback ni penalidad; el
   * front consulta de nuevo (HTTP 202).
   */
  record StillVerifying(Long uploadId) implements UploadCompletionOutcome {}
}
