package com.guille.media.bff.experience.media.application;

import com.guille.media.bff.experience.media.application.port.MediaDeletion;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;

import reactor.core.publisher.Mono;

/**
 * "Eliminar esta media": borra la entrada de catálogo de forma IDEMPOTENTE.
 *
 * <p>Sin transacción distribuida ACID a propósito: el catálogo (movies) es la
 * única fuente de verdad de existencia. Un LOCAL (o DRAFT sin contenido) se
 * borra sin tocar storage. Un MANAGED queda BLOQUEADO hasta que exista
 * compensación durable (outbox/evento o estado DELETING con reintentos): no
 * se hace una llamada frágil movies→storage ni se deja el objeto huérfano.
 *
 * <p>Idempotencia: borrar una media ya inexistente (o no visible) responde
 * igual de bien (204), sin revelar existencia.
 */
@Service
@RequiredArgsConstructor
public class DeleteMedia {

  private final MediaDeletion deletion;

  public Mono<DeletionOutcome> execute(long mediaId) {
    return this.deletion.requestDeletion(mediaId);
  }
}
