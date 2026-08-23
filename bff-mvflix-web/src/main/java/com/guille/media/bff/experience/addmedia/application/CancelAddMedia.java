package com.guille.media.bff.experience.addmedia.application;

import com.guille.media.bff.experience.addmedia.application.port.AddMediaMovies;
import com.guille.media.bff.experience.addmedia.application.port.AddMediaProcessRepository;
import com.guille.media.bff.experience.addmedia.application.port.AddMediaStorage;
import com.guille.media.bff.experience.addmedia.model.AddMediaId;
import com.guille.media.bff.experience.addmedia.model.AddMediaProcess;
import com.guille.media.bff.experience.addmedia.model.AddMediaPhase;
import com.guille.media.bff.experience.addmedia.model.InvalidAddMediaTransition;
import com.guille.media.bff.experience.addmedia.web.AddMediaView;

import lombok.extern.slf4j.Slf4j;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import reactor.core.publisher.Mono;

/**
 * Cancelación del alta por el usuario. Compensaciones acotadas al proceso:
 * cancela la sesión de upload (restaura cuota en storage) y descarta el draft,
 * SOLO si la fase todavía lo permite. READY y CANCELLED son terminales.
 */
@Slf4j
@Service
public class CancelAddMedia {

  private final AddMediaProcessRepository processes;
  private final AddMediaStorage storage;
  private final AddMediaMovies movies;

  public CancelAddMedia(
      AddMediaProcessRepository processes,
      AddMediaStorage storage,
      AddMediaMovies movies) {
    this.processes = processes;
    this.storage = storage;
    this.movies = movies;
  }

  public Mono<AddMediaView> handle(String ownerSubject, String addMediaId) {
    return this.processes
        .findById(new AddMediaId(addMediaId))
        .filter(process -> process.ownedBy(ownerSubject))
        .switchIfEmpty(Mono.error(
            new ResponseStatusException(HttpStatus.NOT_FOUND, "Proceso no encontrado")))
        .flatMap(this::cancelIfAllowed);
  }

  private Mono<AddMediaView> cancelIfAllowed(AddMediaProcess process) {
    if (process.phase() == AddMediaPhase.READY
        || process.phase() == AddMediaPhase.CANCELLED
        || process.phase() == AddMediaPhase.FAILED) {
      return Mono.error(new InvalidAddMediaTransition(
          process.phase(), AddMediaPhase.CANCELLED));
    }
    Mono<Void> cancelUpload = process.uploadId() == null
        ? Mono.empty()
        : this.storage.cancelUpload(process.uploadId());
    Mono<Void> discardDraft = (process.movieId() != null
            && process.phase() == AddMediaPhase.WAITING_FOR_UPLOAD)
        ? this.movies.discardDraft(process.movieId())
            .onErrorResume(err -> {
              // El draft huérfano es recuperable por el operador; no bloquea
              // la cancelación de experiencia del usuario.
              log.warn("add-media cancel: draft {} no pudo descartarse: {}",
                  process.movieId(), err.getMessage());
              return Mono.empty();
            })
        : Mono.empty();

    return cancelUpload
        .then(discardDraft)
        .then(this.processes.save(process.cancelled()))
        .map(AddMediaView::from);
  }
}
