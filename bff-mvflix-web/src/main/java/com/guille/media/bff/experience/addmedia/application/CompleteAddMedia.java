package com.guille.media.bff.experience.addmedia.application;

import com.guille.media.bff.app.dto.CompleteMovieRequest;
import com.guille.media.bff.app.dto.MovieDto;
import com.guille.media.bff.app.dto.UploadStatusDto;
import com.guille.media.bff.app.ports.MoviesWebClient;
import com.guille.media.bff.app.ports.StorageWebClient;
import com.guille.media.bff.app.ports.UsersWebPort;

import lombok.extern.slf4j.Slf4j;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import reactor.core.publisher.Mono;

/**
 * Flujo de cierre del alta de contenido: verifica el estado REAL del upload en
 * storage (el objeto puede llegar por webhook en cualquier momento), clasifica
 * un veredicto y ejecuta rollback + penalidad cuando corresponde.
 *
 * <p>Extraído de WebMoviesService: la coordinación de alta no debe vivir
 * mezclada con catálogo/playback/visibilidad.
 */
@Slf4j
@Service
public class CompleteAddMedia {

  private final MoviesWebClient moviesWebClient;
  private final StorageWebClient storageWebClient;
  private final UsersWebPort usersWebPort;

  public CompleteAddMedia(
      MoviesWebClient moviesWebClient,
      StorageWebClient storageWebClient,
      UsersWebPort usersWebPort) {
    this.moviesWebClient = moviesWebClient;
    this.storageWebClient = storageWebClient;
    this.usersWebPort = usersWebPort;
  }

  /**
   * Complete orquestado: idempotente (si ya está READY responde sin tocar nada)
   * y con veredicto clasificado a partir del estado real del storage.
   *
   * <p>PENDING NO es fallo: una demora del webhook o la verificación asíncrona
   * del storage se traduce en {@link UploadCompletionOutcome.StillVerifying}
   * para que el front consulte de nuevo. Nunca se borra Movie ni el objeto por
   * una espera, y nunca se penaliza al usuario por un tiempo.
   */
  public Mono<UploadCompletionOutcome> complete(Long movieId, CompleteMovieRequest request) {
    log.info("complete: movie={} storageId={} sizeBytes={}",
        movieId, request.storageId(), request.sizeBytes());
    return this.moviesWebClient
        .movieById(movieId)
        .flatMap(movie -> {
          if ("READY".equals(movie.status())) {
            log.info("complete: movie={} ya READY, no-op idempotente", movieId);
            return Mono.just((UploadCompletionOutcome)
                new UploadCompletionOutcome.Completed(movie));
          }
          return this.completeFromDraft(movieId, request);
        });
  }

  private Mono<UploadCompletionOutcome> completeFromDraft(Long movieId,
      CompleteMovieRequest request) {
    return this.storageWebClient
        .uploadStatus(request.storageId())
        .flatMap(status -> this.evaluateStatus(movieId, request, status))
        .onErrorResume(UploadVerdictException.class,
            ex -> this.rollback(movieId, request, ex.getCode(), ex.getMessage(), true))
        .onErrorResume(WebClientResponseException.class,
            ex -> this.downstreamUnavailable(movieId, ex))
        .onErrorResume(WebClientRequestException.class,
            ex -> this.downstreamUnreachable(movieId, ex))
        .doOnError(err -> log.warn("complete: movie={} terminó en error: {}", movieId,
            err.getMessage()));
  }

  private Mono<UploadCompletionOutcome> evaluateStatus(Long movieId,
      CompleteMovieRequest request, UploadStatusDto status) {
    String state = status.status() == null ? "" : status.status();
    log.debug("add-media complete: movie={} estado storage={} key={}", movieId, state, status.storageKey());
    switch (state) {
      case "COMPLETED":
        if (request.sizeBytes() != null
            && status.object() != null
            && status.object().expectedSize() > 0
            && !request.sizeBytes().equals(status.object().expectedSize())) {
          log.warn("complete: movie={} tamaño no cuadra: esperado={} reportado={}",
              movieId, status.object().expectedSize(), request.sizeBytes());
          return Mono.error(new UploadVerdictException("UPLOAD_INCONSISTENT",
              "El tamaño del objeto subido no coincide con lo reportado por el front"));
        }
        return this.persistReady(movieId, request, status);
      case "PENDING":
        log.info("complete: movie={} upload aún PENDING; verificación asíncrona", movieId);
        Long uploadId = this.toStorageId(status.uploadId());
        return Mono.just(new UploadCompletionOutcome.StillVerifying(uploadId));
      case "FAILED":
        log.warn("complete: movie={} storage FAILED: objeto descartado o en cuarentena",
            movieId);
        return Mono.error(new UploadVerdictException("UPLOAD_FAILED",
            "La subida fue marcada como fallida por el storage"));
      default:
        log.warn("complete: movie={} estado inesperado={}", movieId, state);
        return Mono.error(new UploadVerdictException("UPLOAD_INCONSISTENT",
            "Estado inesperado del storage: " + state));
    }
  }

  private Mono<UploadCompletionOutcome> persistReady(Long movieId, CompleteMovieRequest request,
      UploadStatusDto status) {
    log.info("complete: movie={} veredicto OK, persistiendo READY con object_key={}",
        movieId, status.storageKey());
    return this.moviesWebClient
        .completeMovie(movieId, this.toStorageId(status.uploadId()), status.storageKey())
        .doOnSuccess(movie -> log.info("complete: movie={} READY persistida", movieId))
        .map((MovieDto movie) -> (UploadCompletionOutcome)
            new UploadCompletionOutcome.Completed(movie))
        .onErrorResume(WebClientResponseException.class,
            ex -> this.onMoviesCompleteError(movieId, request, ex));
  }

  private Long toStorageId(String uploadId) {
    try {
      return uploadId == null ? null : Long.valueOf(uploadId);
    } catch (NumberFormatException e) {
      log.warn("complete: uploadId no numerico={}, object_id quedará null", uploadId);
      return null;
    }
  }

  private Mono<UploadCompletionOutcome> onMoviesCompleteError(Long movieId,
      CompleteMovieRequest request, WebClientResponseException ex) {
    if (ex.getStatusCode().value() == 404) {
      log.warn("complete: movie={} no existe al persistir; rollback de objeto", movieId);
      return this.rollback(movieId, request, "MOVIE_MISSING",
          "La película no existe al momento del complete", false);
    }
    if (ex.getStatusCode().value() == 409) {
      // Reconciliación: otro camino pudo haber completado la película.
      return this.moviesWebClient
          .movieById(movieId)
          .flatMap(movie -> "READY".equals(movie.status())
              ? Mono.just((UploadCompletionOutcome)
                  new UploadCompletionOutcome.Completed(movie))
              : Mono.<UploadCompletionOutcome>error(new UploadVerdictException(
                  "UPLOAD_CONFLICT",
                  "La película no pudo completarse: estado " + movie.status())))
          .switchIfEmpty(Mono.error(new UploadVerdictException("MOVIE_MISSING",
              "La película no existe al reconciliar el conflicto")));
    }
    return this.downstreamUnavailable(movieId, ex);
  }

  /** Rollback: elimina la película + el objeto (restaura cuota). Opcionalmente penaliza. */
  private Mono<UploadCompletionOutcome> rollback(Long movieId, CompleteMovieRequest request,
      String code, String reason, boolean penalize) {
    log.warn("ROLLBACK movie={} storageId={} code={} reason={}",
        movieId, request.storageId(), code, reason);
    return Mono.when(
            this.moviesWebClient.deleteMovie(movieId).onErrorResume(
                err -> this.logRollbackFailure("película " + movieId, err)),
            this.storageWebClient.deleteObject(request.storageId()).onErrorResume(
                err -> this.logRollbackFailure("objeto storageId=" + request.storageId(), err)))
        .then(Mono.defer(() -> {
          if (penalize) {
            return this.usersWebPort
                .reportViolation(code + ": " + reason)
                .doOnSuccess(v -> log.warn("ROLLBACK: violación registrada para movie={} ({})",
                    movieId, code))
                .onErrorResume(err -> this.logRollbackFailure("violación de movie " + movieId,
                    err))
                .then(Mono.error(
                    new UploadOrchestrationException(HttpStatus.CONFLICT, code, reason)));
          }
          return Mono.error(
              new UploadOrchestrationException(HttpStatus.CONFLICT, code, reason));
        }));
  }

  /** Servicio aguas abajo no disponible: sin rollback, el front puede reintentar. */
  private Mono<UploadCompletionOutcome> downstreamUnavailable(Long movieId,
      WebClientResponseException ex) {
    log.error("complete: movie={} servicio aguas abajo no disponible: status={} {}",
        movieId, ex.getStatusCode(), ex.getMessage());
    return Mono.error(new UploadOrchestrationException(
        HttpStatus.valueOf(ex.getStatusCode().value()), "DOWNSTREAM_UNAVAILABLE",
        "Servicio aguas abajo no disponible: " + ex.getStatusCode()));
  }

  /** Servicio aguas abajo inalcanzable (conexión): sin rollback, el front puede reintentar. */
  private Mono<UploadCompletionOutcome> downstreamUnreachable(Long movieId,
      WebClientRequestException ex) {
    log.error("complete: movie={} servicio aguas abajo inalcanzable: {}", movieId,
        ex.getMessage());
    return Mono.error(new UploadOrchestrationException(HttpStatus.SERVICE_UNAVAILABLE,
        "DOWNSTREAM_UNREACHABLE",
        "Servicio aguas abajo inalcanzable: " + ex.getMessage()));
  }

  private Mono<Void> logRollbackFailure(String what, Throwable err) {
    log.error("ROLLBACK: fallo al borrar {}: {}", what, err.getMessage());
    return Mono.empty();
  }
}
