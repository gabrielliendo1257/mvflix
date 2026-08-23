package com.guille.media.bff.experience.addmedia.application;

import com.guille.media.bff.experience.addmedia.application.port.AddMediaProcessRepository;
import com.guille.media.bff.experience.addmedia.application.port.AddMediaMovies;
import com.guille.media.bff.experience.addmedia.model.AddMediaProcess;
import com.guille.media.bff.experience.addmedia.application.port.AddMediaStorage;
import com.guille.media.bff.experience.addmedia.model.AddMediaId;
import com.guille.media.bff.experience.addmedia.model.AddMediaPhase;
import com.guille.media.bff.experience.addmedia.model.InvalidAddMediaTransition;
import com.guille.media.bff.experience.addmedia.web.AddMediaView;
import com.guille.media.bff.shared.error.EntityNotFound;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;

import reactor.core.publisher.Mono;

/**
 * Cancelación race-safe: PRIMERO reclama CANCELLING (CAS) para bloquear
 * complete/cancel competidores, DESPUÉS compensa recursos, al final persiste
 * CANCELLED. El descarte del draft solo aplica mientras WAITING_FOR_UPLOAD:
 * en VERIFYING el contenido ya fue verificado por storage.
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class CancelAddMedia {

  private final AddMediaProcessRepository processes;
  private final AddMediaStorage storage;
  private final AddMediaMovies movies;

  public Mono<AddMediaView> handle(String ownerSubject, String addMediaId) {
    return this.processes
        .findById(new AddMediaId(addMediaId))
        .filter(process -> process.ownedBy(ownerSubject))
        .switchIfEmpty(Mono.error(new EntityNotFound("Proceso no encontrado")))
        .flatMap(this::cancelIfAllowed);
  }

  private Mono<AddMediaView> cancelIfAllowed(AddMediaProcess process) {
    if (process.phase() == AddMediaPhase.READY
        || process.phase() == AddMediaPhase.CANCELLED
        || process.phase() == AddMediaPhase.FAILED
        || process.phase() == AddMediaPhase.STARTING
        || process.phase() == AddMediaPhase.PREPARING) {
      return Mono.error(new InvalidAddMediaTransition(
          process.phase(), AddMediaPhase.CANCELLING));
    }
    boolean wasWaiting = process.phase() == AddMediaPhase.WAITING_FOR_UPLOAD;
    return this.processes
        .tryCancelClaim(process.id())
        .flatMap(claimed -> {
          if (!claimed) {
            // READY/CANCELLED/FAILED ganó la carrera: nada que compensar.
            return Mono.error(new InvalidAddMediaTransition(
                process.phase(), AddMediaPhase.CANCELLING));
          }
          return this.compensate(process, wasWaiting)
              .then(this.processes.save(process.cancelling().cancelled()))
              .map(AddMediaView::from);
        });
  }

  /** Compensaciones best-effort; los fallos quedan registrados como pendientes. */
  private Mono<Void> compensate(AddMediaProcess process, boolean wasWaiting) {
    Mono<Void> cancelUpload = process.uploadId() == null
        ? Mono.empty()
        : this.storage.cancelUpload(process.uploadId())
            .onErrorResume(err -> {
              log.error("add-media cancel: PENDIENTE liberar upload {}: {}",
                  process.uploadId(), err.getMessage());
              return Mono.empty();
            });
    Mono<Void> discardDraft = (wasWaiting && process.movieId() != null)
        ? this.movies.discardDraft(process.movieId())
            .onErrorResume(err -> {
              log.error("add-media cancel: PENDIENTE descartar draft {}: {}",
                  process.movieId(), err.getMessage());
              return Mono.empty();
            })
        : Mono.empty();
    return cancelUpload.then(discardDraft);
  }
}
