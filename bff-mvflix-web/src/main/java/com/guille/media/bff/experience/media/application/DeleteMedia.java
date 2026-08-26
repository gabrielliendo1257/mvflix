package com.guille.media.bff.experience.media.application;

import com.guille.media.bff.experience.media.application.port.MediaDeletion;
import com.guille.media.bff.experience.media.application.port.MediaDetailProjection;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

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
@Slf4j
@Service
@RequiredArgsConstructor
public class DeleteMedia {

  private final MediaDeletion deletion;
  private final MediaDetailProjection projection;

  public Mono<Void> execute(long mediaId) {
    return this.projection.detail(mediaId)
        .flatMap(detail -> this.guardAndDelete(mediaId, detail))
        .onErrorResume(MediaDetailNotFoundException.class, error -> {
          log.info("media {} ya no existía (no-op idempotente)", mediaId);
          return Mono.empty();
        });
  }

  private Mono<Void> guardAndDelete(long mediaId, MediaDetail detail) {
    String source = detail.access().source();
    if ("MANAGED".equals(source) || "INVALID".equals(source)) {
      log.warn("delete bloqueado: media {} es {} (sin compensación durable de storage)",
          mediaId, source);
      return Mono.error(new ManagedDeleteBlockedException(mediaId, source));
    }
    return this.deletion.deleteCatalog(mediaId)
        .doOnNext(deleted -> log.info(
            "media {} {}", mediaId, deleted ? "borrada" : "ya no existía"))
        .then();
  }
}
