package com.guille.media.bff.experience.addmedia.application;

import com.guille.media.bff.app.dto.CompleteMovieRequest;
import com.guille.media.bff.experience.addmedia.application.UploadCompletionOutcome.Completed;
import com.guille.media.bff.experience.addmedia.application.port.AddMediaProcessRepository;
import com.guille.media.bff.experience.addmedia.model.AddMediaId;
import com.guille.media.bff.experience.addmedia.model.InvalidAddMediaTransition;
import com.guille.media.bff.experience.addmedia.model.AddMediaPhase;
import com.guille.media.bff.experience.addmedia.model.AddMediaProcess;
import com.guille.media.bff.experience.addmedia.web.AddMediaView;

import lombok.extern.slf4j.Slf4j;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import reactor.core.publisher.Mono;

/**
 * Cierre de la experiencia Add Media: una sola intención (complete) que
 * coordina la verificación del upload y la persistencia READY en Movies,
 * manteniendo la fase del proceso alineada con el resultado.
 *
 * <p>PENDING → VERIFYING (202, sin rollback): puede ser demora del webhook.
 * FAILED/INCONSISTENT → FAILED en el proceso; el rollback + penalidad lo
 * decide CompleteAddMedia según veredicto del storage.
 */
@Slf4j
@Service
public class CompleteProcessAddMedia {

  private final AddMediaProcessRepository processes;
  private final CompleteAddMedia completion;

  public CompleteProcessAddMedia(
      AddMediaProcessRepository processes,
      CompleteAddMedia completion) {
    this.processes = processes;
    this.completion = completion;
  }

  public Mono<AddMediaView> handle(String ownerSubject, String addMediaId, Long reportedSizeBytes) {
    return this.processes
        .findById(new AddMediaId(addMediaId))
        .filter(process -> process.ownedBy(ownerSubject))
        .switchIfEmpty(Mono.error(
            new ResponseStatusException(HttpStatus.NOT_FOUND, "Proceso no encontrado")))
        .flatMap(process -> {
          // Idempotencia: READY responde siempre la misma vista.
          if (process.phase() == AddMediaPhase.READY) {
            return Mono.just(AddMediaView.from(process));
          }
          // Terminales no reabren; sin upload preparado no hay qué verificar.
          if (process.phase() == AddMediaPhase.CANCELLED
              || process.phase() == AddMediaPhase.FAILED
              || process.phase() == AddMediaPhase.STARTING
              || process.phase() == AddMediaPhase.PREPARING) {
            return Mono.error(new InvalidAddMediaTransition(
                process.phase(), AddMediaPhase.VERIFYING_UPLOAD));
          }
          return this.completeViaStorage(process, reportedSizeBytes);
        });
  }

  private Mono<AddMediaView> completeViaStorage(AddMediaProcess process,
      Long reportedSizeBytes) {
    // Reintento en VERIFYING: la fase ya es correcta, no re-persistir.
    Mono<AddMediaProcess> ensureVerifying =
        process.phase() == AddMediaPhase.VERIFYING_UPLOAD
            ? Mono.just(process)
            : this.processes.save(process.verifying());
    return ensureVerifying
        .flatMap(verify ->
            this.completion.complete(process.movieId(),
                    new CompleteMovieRequest(process.uploadId(), reportedSizeBytes))
                .flatMap(outcome -> {
                  if (outcome instanceof Completed completed) {
                    return this.processes
                        .save(verify.ready())
                        .map(saved -> withMovie(AddMediaView.from(saved), completed.movie()));
                  }
                  return Mono.just(AddMediaView.from(verify));
                })
                .onErrorResume(
                    error -> error instanceof UploadOrchestrationException orchestration
                        && orchestration.getStatus() == HttpStatus.CONFLICT,
                    error -> this.processes
                        .save(verify.failed(failureCodeOf(error)))
                        .then(Mono.error(error))));
  }

  private static AddMediaView withMovie(AddMediaView view,
      com.guille.media.bff.app.dto.MovieDto movie) {
    return new AddMediaView(view.addMediaId(), view.ownerSubject(), view.phase(),
        movie.id(), view.uploadId(), view.upload(), view.failureCode());
  }

  private static String failureCodeOf(Throwable error) {
    if (error instanceof UploadOrchestrationException orchestration) {
      return orchestration.getCode();
    }
    return "ADD_MEDIA_FAILED";
  }
}
