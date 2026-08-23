package com.guille.media.bff.experience.addmedia.application;

import com.guille.media.bff.app.dto.UploadCreateRequest;
import com.guille.media.bff.app.dto.UploadSessionDto;
import com.guille.media.bff.app.dto.UserProfile;
import com.guille.media.bff.app.ports.UsersWebPort;
import com.guille.media.bff.experience.addmedia.application.port.AddMediaMovies;
import com.guille.media.bff.experience.addmedia.application.port.AddMediaMovies.IdentifiedDraft;
import com.guille.media.bff.experience.addmedia.application.port.AddMediaProcessRepository;
import com.guille.media.bff.experience.addmedia.application.port.AddMediaStorage;
import com.guille.media.bff.experience.addmedia.model.AddMediaPhase;
import com.guille.media.bff.experience.addmedia.model.AddMediaProcess;
import com.guille.media.bff.experience.addmedia.web.AddMediaView;
import com.guille.media.bff.experience.addmedia.web.StartAddMediaRequest;

import lombok.extern.slf4j.Slf4j;

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
        .createIfAbsent(ownerSubject, request.idempotencyKey(), fingerprintOf(request))
        .flatMap(process -> {
          if (process.phase() == AddMediaPhase.WAITING_FOR_UPLOAD
              && process.uploadId() != null) {
            // Replay tras recarga/pérdida de la primera respuesta: restaurar
            // las instrucciones de subida con un presigned fresco.
            return this.storage.refreshInstructions(process.uploadId())
                .map(session -> AddMediaView.waitingForUpload(process, session))
                .onErrorResume(error -> {
                  log.warn("add-media replay: no se pudo renovar URL para {}: {}",
                      process.id(), error.getMessage());
                  return Mono.just(AddMediaView.from(process));
                });
          }
          if (process.phase() != AddMediaPhase.STARTING) {
            log.info("add-media replay: process={} phase={}",
                process.id(), process.phase());
            return Mono.just(AddMediaView.from(process));
          }
          // Claim atómico: solo un request ejecuta los side effects.
          return this.processes
              .tryClaim(process.id())
              .flatMap(claimed -> {
                if (!claimed) {
                  // Otro request está preparando: el front consulta estado.
                  log.info("add-media: proceso {} reclamado por otro request",
                      process.id());
                  return this.processes
                      .findById(process.id())
                      .map(AddMediaView::from);
                }
                // Continuar sobre la instancia ya reclamada (PREPARING).
                return this.processes
                    .findById(process.id())
                    .flatMap(claimedProcess ->
                        this.startFresh(claimedProcess, request)
                            .onErrorResume(error ->
                                this.processes.releaseClaim(process.id())
                                    .then(Mono.error(error))));
              });
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
      return Mono.error(new UserBlockedException(
          profile.username() == null ? "?" : profile.username(), profile.violations()));
    }
    return Mono.empty();
  }

  private Mono<AddMediaView> createDraftAndUpload(AddMediaProcess process,
      StartAddMediaRequest request) {
    // Default EXPLÍCITO: la ausencia de intención es PRIVATE.
    var access = request.access() == null
        ? new StartAddMediaRequest.InitialAccess("PRIVATE", java.util.List.of())
        : request.access();
    IdentifiedDraft identified = new IdentifiedDraft(
        request.movie().draft(),
        request.movie().providerId(),
        access.visibility() == null ? "PRIVATE" : access.visibility(),
        access.sharedWith());
    return this.movies
        .createIdentifiedDraft(identified)
        .flatMap(
            draft ->
                this.prepareUpload(process, draft.id(), request)
                    // El upload ya se compensó dentro de prepareUpload (si
                    // llegó a existir); aquí solo queda el draft.
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
        .flatMap(session -> {
          Long uploadId = strictParseUploadId(session.uploadId());
          // Con uploadId no fiable NO se persiste: se compensa todo.
          AddMediaProcess prepared = process.uploadPrepared(movieId, uploadId);
          return this.persistPrepared(prepared, session)
              .onErrorResume(err ->
                  this.compensateUpload(uploadId).then(Mono.error(err)))
              .doOnNext(view -> log.info("add-media started: process={} movie={} upload={}",
                  view.addMediaId(), view.movieId(), view.uploadId()));
        });
  }

  private Mono<AddMediaView> persistPrepared(
      AddMediaProcess process, UploadSessionDto session) {
    return this.processes
        .save(process)
        .map(saved -> AddMediaView.waitingForUpload(saved, session));
  }

  private static Long strictParseUploadId(String uploadId) {
    if (uploadId == null || !uploadId.chars().allMatch(Character::isDigit)) {
      throw new InvalidStorageResponseException(
          "Storage devolvió un uploadId no válido: " + uploadId);
    }
    try {
      return Long.valueOf(uploadId);
    } catch (NumberFormatException e) {
      throw new InvalidStorageResponseException(
          "Storage devolvió un uploadId no válido: " + uploadId);
    }
  }

  /** Compensación best-effort: libera la cuota reservada por la sesión. */
  private Mono<Void> compensateUpload(Long uploadId) {
    log.warn("add-media: fallo tras crear el upload {}; cancelando sesión", uploadId);
    return this.storage
        .cancelUpload(uploadId)
        .onErrorResume(err -> {
          log.error("add-media: compensación PENDIENTE - upload {} no pudo cancelarse: {}",
              uploadId, err.getMessage());
          return Mono.empty();
        });
  }

  /** Huella canónica del intento; pública para que los tests usen la MISMA. */
  static String fingerprintOf(StartAddMediaRequest request) {
    return RequestFingerprint.of(java.util.Map.of(
        "file", request.file(),
        "movie", java.util.Map.of(
            "providerId", request.movie().providerId() == null ? "" : request.movie().providerId(),
            "draft", request.movie().draft()),
        "access", accessOf(request)));
  }

  private static StartAddMediaRequest.InitialAccess accessOf(StartAddMediaRequest request) {
    return request.access() == null
        ? new StartAddMediaRequest.InitialAccess("PRIVATE", java.util.List.of())
        : request.access();
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

}
