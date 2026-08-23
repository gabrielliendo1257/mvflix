package com.guille.media.bff.experience.addmedia.application;

import com.guille.media.bff.app.dto.UploadCreateRequest;
import com.guille.media.bff.app.dto.UploadSessionDto;
import com.guille.media.bff.app.dto.UserProfile;
import com.guille.media.bff.app.ports.UsersWebPort;
import com.guille.media.bff.experience.addmedia.application.port.AddMediaMovies;
import com.guille.media.bff.experience.addmedia.application.port.AddMediaProcessRepository;
import com.guille.media.bff.experience.addmedia.application.port.AddMediaStorage;
import com.guille.media.bff.experience.addmedia.model.AddMediaPhase;
import com.guille.media.bff.experience.addmedia.model.AddMediaProcess;
import com.guille.media.bff.experience.addmedia.web.AddMediaView;
import com.guille.media.bff.experience.addmedia.web.StartAddMediaRequest;

import lombok.extern.slf4j.Slf4j;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import reactor.core.publisher.Mono;

/**
 * Inicio idempotente del alta: primero el DRAFT en Movies (dueño del catálogo
 * y de la política de usuario bloqueado), después la reserva en Storage.
 *
 * <p>Compensación: si Storage falla tras crear el draft, se descarta SOLO el
 * draft creado por este proceso y el error se propaga; el proceso queda en
 * STARTING para que un replay con la misma idempotencyKey reintente limpio.
 * Sin transacción distribuida: compensaciones explícitas y acotadas.
 */
@Slf4j
@Service
public class StartAddMedia {

  private final AddMediaMovies movies;
  private final AddMediaStorage storage;
  private final AddMediaProcessRepository processes;
  private final UsersWebPort users;

  public StartAddMedia(
      AddMediaMovies movies,
      AddMediaStorage storage,
      AddMediaProcessRepository processes,
      UsersWebPort users) {
    this.movies = movies;
    this.storage = storage;
    this.processes = processes;
    this.users = users;
  }

  public Mono<AddMediaView> handle(String ownerSubject, StartAddMediaRequest request) {
    return this.processes
        .createIfAbsent(ownerSubject, request.idempotencyKey())
        .flatMap(process -> {
          if (process.phase() != AddMediaPhase.STARTING) {
            // Replay del mismo intento: no duplicar draft ni upload.
            log.info("add-media replay: process={} phase={}",
                process.id(), process.phase());
            return Mono.just(AddMediaView.from(process));
          }
          return this.startFresh(process, request);
        });
  }

  /**
   * Gate de experiencia: un usuario bloqueado por violaciones no inicia altas.
   * La política vive en users; aquí solo se traduce a un error amigable antes
   * de gastar recursos en drafts/uploads.
   */
  private Mono<AddMediaView> startFresh(AddMediaProcess process, StartAddMediaRequest request) {
    return this.users
        .me()
        .flatMap(profile -> this.guardBlocked(profile)
            // defer: si el gate falla, la continuación ni se construye.
            .then(Mono.defer(() -> this.createDraftAndUpload(process, request))));
  }

  private Mono<Void> guardBlocked(UserProfile profile) {
    if (profile.blocked()) {
      log.warn("add-media bloqueado: usuario={} violaciones={}",
          profile.username(), profile.violations());
      return Mono.error(new UploadOrchestrationException(HttpStatus.FORBIDDEN,
          "USER_BLOCKED", "El usuario está bloqueado por violaciones repetidas"));
    }
    return Mono.empty();
  }

  private Mono<AddMediaView> createDraftAndUpload(AddMediaProcess process,
      StartAddMediaRequest request) {
    return this.movies
        .createDraft(request.movie().draft())
        .flatMap(
            draft ->
                this.prepareUpload(process, draft.id(), request)
                    // Compensación acotada: solo el draft creado por ESTE proceso.
                    .onErrorResume(
                        error -> this.compensateDraft(draft.id()).then(Mono.error(error))));
  }

  private Mono<AddMediaView> prepareUpload(
      AddMediaProcess process, Long movieId, StartAddMediaRequest request) {
    UploadCreateRequest uploadRequest =
        new UploadCreateRequest(
            request.file().filename(),
            request.file().sizeBytes(),
            request.file().mimeType());
    return this.storage
        .prepareUpload(uploadRequest)
        .flatMap(session -> this.persistPrepared(process, movieId, session))
        .doOnNext(view -> log.info("add-media started: process={} movie={} upload={}",
            view.addMediaId(), view.movieId(), view.uploadId()));
  }

  private Mono<AddMediaView> persistPrepared(
      AddMediaProcess process, Long movieId, UploadSessionDto session) {
    Long uploadId = parseUploadId(session.uploadId());
    return this.processes
        .save(process.uploadPrepared(movieId, uploadId))
        .map(saved -> AddMediaView.waitingForUpload(saved, session));
  }

  /** Compensación best-effort: si el discard falla queda pendiente en el log. */
  private Mono<Void> compensateDraft(Long movieId) {
    log.warn("add-media: storage falló tras crear el draft {}; descartando draft", movieId);
    return this.movies
        .discardDraft(movieId)
        .onErrorResume(err -> {
          log.error("add-media: compensación PENDIENTE - draft {} no pudo descartarse: {}",
              movieId, err.getMessage());
          return Mono.empty();
        });
  }

  private static Long parseUploadId(String uploadId) {
    try {
      return uploadId == null ? null : Long.valueOf(uploadId);
    } catch (NumberFormatException e) {
      return null;
    }
  }
}
