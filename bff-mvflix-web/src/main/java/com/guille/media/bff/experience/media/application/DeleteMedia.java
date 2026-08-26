package com.guille.media.bff.experience.media.application;

import com.guille.media.bff.experience.media.application.port.MediaDeletion;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;

import reactor.core.publisher.Mono;

/**
 * "Eliminar esta media": borra la entrada de catálogo de forma IDEMPOTENTE.
 *
 * <p>Sin transacción distribuida ACID a propósito: el catálogo (movies) es la
 * única fuente de verdad de existencia. El objeto MANAGED en storage no se
 * borra en este camino; queda huérfano y su limpieza durable (encolar en la
 * cola de huérfanos de storage) es un TODO posterior a esta operación. Un
 * retry sobre media ya borrada responde igual de bien (204).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DeleteMedia {

  private final MediaDeletion deletion;

  public Mono<Void> execute(long mediaId) {
    return this.deletion.deleteCatalog(mediaId)
        .doOnNext(deleted -> log.info(
            "media {} {}", mediaId, deleted ? "borrada" : "ya no existía (no-op idempotente)"))
        .then();
  }
}
