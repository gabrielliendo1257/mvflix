package com.guille.media.bff.app.service;

import com.guille.media.bff.app.dto.BulkVisibilityRequest;
import com.guille.media.bff.app.dto.CompleteMovieRequest;
import com.guille.media.bff.app.dto.CreateMovieRequest;
import com.guille.media.bff.app.dto.MediaAssetDto;
import com.guille.media.bff.app.dto.MovieDetailDto;
import com.guille.media.bff.app.dto.MovieDetailDto.PlaybackDto;
import com.guille.media.bff.app.dto.MovieDto;
import com.guille.media.bff.app.dto.MovieEnrichmentPreviewDto;
import com.guille.media.bff.app.dto.MovieEnrichmentSearchDto;
import com.guille.media.bff.app.dto.MovieListItemDto;
import com.guille.media.bff.app.dto.MovieUpdateRequest;
import com.guille.media.bff.app.dto.StreamTicketDto;
import com.guille.media.bff.app.dto.UploadStatusDto;
import com.guille.media.bff.app.ports.MoviesWebClient;
import com.guille.media.bff.app.ports.StorageWebClient;
import com.guille.media.bff.app.ports.UsersWebPort;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.server.ResponseStatusException;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Orquestador del flujo de alta de película. El front solo llama al complete; el BFF
 * verifica el estado real del storage (el objeto llega por webhook), clasifica el veredicto,
 * persiste la película y, si algo falla, hace rollback (película + objeto + cuota) y
 * registra penalidades cuando el caso lo amerita.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WebMoviesService {

  private static final int PENDING_RETRIES = 3;

  /** Tamaño de lote interno del BFF hacia mvflix-movies (progreso por SSE). */
  private static final int BULK_CHUNK_SIZE = 25;

  /** Chunks de visibilidad en vuelo hacia mvflix-movies (velocidad sin saturar la DB). */
  private static final int BULK_PARALLELISM = 4;

  private final MoviesWebClient moviesWebClient;
  private final StorageWebClient storageWebClient;
  private final UsersWebPort usersWebPort;
  private final StreamTicketService streamTicketService;
  private final JobStore jobStore;

  public Flux<MovieListItemDto> list(int limit) {
    return this.moviesWebClient
        .listMovies(limit)
        .map(movie -> new MovieListItemDto(
            movie.id(), movie.status(), movie.visibility(), movie.kind(), movie.title(),
            movie.year(), movie.posterPath()));
  }

  /**
   * Detalle orientado a la vista: metadata de movies + disponibilidad de reproducción
   * desde storage (URL firmada). Si storage no responde, la pantalla degrada a
   * {@code playback.available=false} en lugar de romper.
   */
  public Mono<MovieDetailDto> detail(Long movieId) {
    return this.moviesWebClient
        .movieById(movieId)
        .flatMap(movie -> this.playbackFor(movie)
            .map(playback -> new MovieDetailDto(movie, playback)));
  }

  private Mono<PlaybackDto> playbackFor(MovieDto movie) {
    if (movie.objectId() == null) {
      return this.localPlaybackFor(movie);
    }
    return this.storageWebClient
        .stream(String.valueOf(movie.objectId()))
        .map(session -> new PlaybackDto(true, session.streamingUrl()))
        .doOnError(error -> log.warn(
            "detail: movie={} playback no disponible: {}",
            movie.id(), error.getMessage()))
        .onErrorResume(error -> Mono.just(new PlaybackDto(false, null)));
  }

  /**
   * Playback LOCAL (movie de biblioteca del operador): el archivo vive en el
   * filesystem del storage y se sirve con Range via el proxy del BFF.
   * Sin asset asociado (p.ej. DRAFT sin media) degrada a no disponible.
   */
  private Mono<PlaybackDto> localPlaybackFor(MovieDto movie) {
    return this.moviesWebClient
        .assetByMovie(movie.id())
        .map(asset -> new PlaybackDto(true, "/web/movies/" + movie.id() + "/stream"))
        .doOnError(error -> log.warn(
            "detail: movie={} playback LOCAL no disponible: {}",
            movie.id(), error.getMessage()))
        .onErrorResume(error -> Mono.just(new PlaybackDto(false, null)));
  }

  /**
   * Proxy de stream LOCAL: resuelve el asset de la movie y delega en el
   * storage, pasando Range y devolviendo status/headers/cuerpo tal cual.
   */
  public Mono<ResponseEntity<Flux<DataBuffer>>> stream(
      Long movieId, String rangeHeader, String ticket) {
    if (ticket == null || ticket.isBlank()) {
      return this.streamAs(movieId, rangeHeader, null);
    }
    return Mono.defer(() -> Mono.justOrEmpty(this.streamTicketService.resolve(ticket)))
        .flatMap(resolved -> {
          if (!resolved.movieId().equals(movieId)) {
            log.warn("stream: ticket de movie={} usado en movie={}", resolved.movieId(), movieId);
            return Mono.error(new StreamTicketException("Ticket de otra movie"));
          }
          return this.streamAs(movieId, rangeHeader, resolved.userJwt());
        });
  }

  /**
   * Stream bajo la identidad del JWT dado (ticket) o del contexto de seguridad
   * (Bearer o sesión). Con {@code userJwt} se reenvía ese JWT a los backends
   * metiéndolo en el contexto reactivo, donde el filtro outbound lo toma.
   */
  private Mono<ResponseEntity<Flux<DataBuffer>>> streamAs(
      Long movieId, String rangeHeader, String userJwt) {
    Mono<ResponseEntity<Flux<DataBuffer>>> stream =
        this.moviesWebClient
            .assetByMovie(movieId)
            .flatMap(asset -> this.storageWebClient.streamLibraryFile(
                asset.libraryId(), asset.relativePath(), rangeHeader))
            .switchIfEmpty(Mono.defer(() -> {
              log.warn("stream: movie={} sin asset de biblioteca", movieId);
              return Mono.just(ResponseEntity.notFound().build());
            }))
            .onErrorResume(error -> {
              if (error instanceof WebClientResponseException responseException) {
                if (responseException.getStatusCode().value() == HttpStatus.FORBIDDEN.value()) {
                  log.warn("stream: movie={} acceso denegado (403)", movieId);
                  return Mono.error(responseException);
                }
                log.warn("stream: movie={} no disponible: {}", movieId, responseException.getMessage());
                return Mono.just(ResponseEntity.notFound().build());
              }
              log.warn("stream: movie={} no disponible: {}", movieId, error.getMessage());
              return Mono.just(ResponseEntity.notFound().build());
            });
    if (userJwt == null) {
      return stream;
    }
    return stream.contextWrite(ReactiveSecurityContextHolder.withAuthentication(
        new JwtAuthenticationToken(jwtFrom(userJwt))));
  }

  private static org.springframework.security.oauth2.jwt.Jwt jwtFrom(String userJwt) {
    return org.springframework.security.oauth2.jwt.Jwt.withTokenValue(userJwt)
        .header("alg", "RS256")
        .claim("sub", "ticket")
        .build();
  }

  /**
   * Emite un ticket de stream (para el {@code <video>} del front, que no puede
   * mandar el JWT en header). {@code userJwt} llega ya resuelto por el controller
   * (Bearer o access token de sesion); el ticket expira a los pocos minutos.
   */
  public Mono<StreamTicketDto> issueStreamTicket(Long movieId, String userJwt) {
    if (userJwt == null || userJwt.isBlank()) {
      return Mono.error(new StreamTicketException("Autenticación requerida"));
    }
    return this.moviesWebClient
        .movieById(movieId)
        .map(movie -> new StreamTicketDto(
            "/web/movies/" + movie.id() + "/stream?ticket="
                + this.streamTicketService.issue(movie.id(), userJwt)));
  }

  public Mono<List<MovieEnrichmentSearchDto>> search(String query, Integer year) {
    return this.moviesWebClient.searchCandidates(query, year).collectList();
  }

  /** Preview de un candidato sin persistir: el usuario confirma antes del enrich. */
  public Mono<MovieEnrichmentPreviewDto> preview(Long tmdbId) {
    return this.moviesWebClient.previewCandidate(tmdbId);
  }

  /** Autocompletado con el candidato elegido por el usuario. */
  public Mono<MovieDto> enrich(Long movieId, Long tmdbId) {
    return this.moviesWebClient.enrichMovie(movieId, tmdbId);
  }

  /** Desvincula la película del proveedor externo (vuelve a RAW). */
  public Mono<MovieDto> unlinkEnrichment(Long movieId) {
    return this.moviesWebClient.unlinkEnrichment(movieId);
  }

  /**
   * Cambia la visibilidad del catalogo (PUBLIC/PRIVATE/SHARED). El BFF orquesta: en
   * SHARED tambien reemplaza la lista de usuarios compartidos (una sola llamada
   * desde el front); en PUBLIC/PRIVATE usernames se ignora. Solo el dueno.
   */
  public Mono<MovieDto> visibility(Long movieId, String visibility, List<String> usernames) {
    if (visibility == null || visibility.isBlank()) {
      return Mono.error(new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "VISIBILITY_REQUIRED"));
    }
    boolean shared = "SHARED".equals(visibility);
    if (shared && (usernames == null || usernames.isEmpty())) {
      return Mono.error(new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "SHARED_REQUIRES_USERNAMES"));
    }
    return this.moviesWebClient
        .updateVisibility(movieId, visibility)
        .flatMap(movie -> shared ? this.moviesWebClient.updateShares(movieId, usernames) : Mono.just(movie));
  }

  /** Reemplaza la lista de usuarios compartidos; solo el dueño. */
  public Mono<MovieDto> shares(Long movieId, List<String> usernames) {
    return this.moviesWebClient.updateShares(movieId, usernames);
  }

  /**
   * Cambio de visibilidad en lote (selección del front y/o librerías enteras).
   * El trabajo corre en background por lotes de {@link #BULK_CHUNK_SIZE} y se
   * registra como un {@link Job} en {@link JobStore}: el POST responde YA con el
   * estado inicial y el progreso llega por SSE en /web/activity/{id}/events.
   */
  public Mono<Job> bulkVisibility(BulkVisibilityRequest request) {
    String visibility = request.visibility();
    if (visibility == null || visibility.isBlank()) {
      return Mono.error(new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "VISIBILITY_REQUIRED"));
    }
    boolean shared = "SHARED".equals(visibility);
    if (shared && (request.usernames() == null || request.usernames().isEmpty())) {
      return Mono.error(new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "SHARED_REQUIRES_USERNAMES"));
    }
    String jobId = UUID.randomUUID().toString();
    return ReactiveSecurityContextHolder.getContext()
        .map(context -> {
          var auth = context.getAuthentication();
          return auth instanceof JwtAuthenticationToken jwtAuth
              ? jwtAuth.getToken().getTokenValue()
              : "";
        })
        .switchIfEmpty(Mono.just(""))
        .flatMap(token -> {
          Job initial = this.jobStore.start(jobId, JobType.BULK_VISIBILITY);
          this.resolveMovieIds(request)
              .flatMap(ids -> this.runBulkJob(initial, ids, request, token))
              .subscribe(
                  finalState -> log.info("bulkVisibility {} -> {} (total={}, ok={}, fail={})",
                      jobId, visibility, finalState.total(), finalState.done(), finalState.failed()),
                  error -> {
                    log.error("bulkVisibility {} fallo: {}", jobId, error.getMessage(), error);
                    this.jobStore.complete(jobId, initial.failed(0, 0, 0));
                  });
          return Mono.just(initial);
        });
  }

  /**
   * Resuelve los ids pedidos: los directos + los assets identificados de las
   * librerías (para que el total y el progreso sean exactos desde el inicio).
   */
  private Mono<List<Long>> resolveMovieIds(BulkVisibilityRequest request) {
    Flux<Long> direct = request.movieIds() == null ? Flux.empty() : Flux.fromIterable(request.movieIds());
    Flux<Long> fromLibraries = request.libraryIds() == null
        ? Flux.empty()
        : Flux.fromIterable(request.libraryIds())
            .flatMap(libraryId -> this.moviesWebClient.listAssets(libraryId, null))
            .filter(asset -> asset.movieId() != null)
            .map(MediaAssetDto::movieId);
    return Flux.concat(direct, fromLibraries).distinct().collectList();
  }

  private Mono<Job> runBulkJob(
      Job initial, List<Long> movieIds, BulkVisibilityRequest request, String accessToken) {
    String jobId = initial.id();
    int total = movieIds.size();
    if (total == 0) {
      return Mono.defer(() -> {
        Job done = initial.completed(0, 0, 0);
        this.jobStore.complete(jobId, done);
        return Mono.just(done);
      });
    }
    AtomicInteger done = new AtomicInteger(0);
    AtomicInteger failed = new AtomicInteger(0);
    return Flux.fromIterable(chunk(movieIds, BULK_CHUNK_SIZE))
        .flatMap(chunk -> this.moviesWebClient
            .bulkUpdateVisibility(chunk, List.of(), request.visibility(), request.usernames(),
                accessToken)
            .doOnNext(result -> {
              done.addAndGet(result.updated());
              failed.addAndGet(result.failed());
              this.jobStore.update(jobId, initial.progress(total, done.get(), failed.get()));
            }), BULK_PARALLELISM)
        .then(Mono.defer(() -> {
          Job finalState = initial.completed(total, done.get(), failed.get());
          this.jobStore.complete(jobId, finalState);
          return Mono.just(finalState);
        }));
  }

  private static List<List<Long>> chunk(List<Long> ids, int size) {
    List<List<Long>> chunks = new ArrayList<>();
    for (int i = 0; i < ids.size(); i += size) {
      chunks.add(ids.subList(i, Math.min(ids.size(), i + size)));
    }
    return chunks;
  }

  /** Edición manual de la metadata (merge: null conserva el valor actual); solo el dueño. */
  public Mono<MovieDto> updateMovie(Long movieId, MovieUpdateRequest request) {
    return this.moviesWebClient.updateMovie(movieId, request);
  }

  public Mono<MovieDto> create(CreateMovieRequest request) {
    return this.usersWebPort
        .me()
        .flatMap(profile -> {
          if (profile.blocked()) {
            log.warn("create bloqueado: usuario={} violaciones={}",
                profile.username(), profile.violations());
            return Mono.error(new UploadOrchestrationException(HttpStatus.FORBIDDEN,
                "USER_BLOCKED", "El usuario está bloqueado por violaciones repetidas"));
          }
          log.info("create: usuario={} title={}", profile.username(), request.title());
          return this.moviesWebClient.createMovie(request);
        });
  }

  /**
   * Complete orquestado: idempotente (si ya está READY responde sin tocar nada) y con
   * veredicto clasificado a partir del estado real del storage.
   */
  public Mono<MovieDto> complete(Long movieId, CompleteMovieRequest request) {
    log.info("complete: movie={} storageId={} sizeBytes={}",
        movieId, request.storageId(), request.sizeBytes());
    return this.moviesWebClient
        .movieById(movieId)
        .flatMap(movie -> {
          if ("READY".equals(movie.status())) {
            log.info("complete: movie={} ya READY, no-op idempotente", movieId);
            return Mono.just(movie);
          }
          return this.completeFromDraft(movieId, request);
        });
  }

  private Mono<MovieDto> completeFromDraft(Long movieId, CompleteMovieRequest request) {
    return this.storageWebClient
        .uploadStatus(request.storageId())
        .flatMap(status -> this.evaluateStatus(movieId, request, status))
        .retryWhen(
            Retry.backoff(PENDING_RETRIES, Duration.ofMillis(500))
                .maxBackoff(Duration.ofSeconds(2))
                .filter(PendingUploadException.class::isInstance)
                .doBeforeRetry(signal ->
                    log.info("complete: movie={} storage PENDING, reintento {}/{}",
                        movieId, signal.totalRetries() + 1, PENDING_RETRIES)))
        .onErrorResume(PendingUploadException.class,
            ex -> this.rollback(movieId, request, "UPLOAD_PENDING",
                "El objeto no llegó a COMPLETED tras " + PENDING_RETRIES + " reintentos", false))
        .onErrorResume(UploadVerdictException.class,
            ex -> this.rollback(movieId, request, ex.getCode(), ex.getMessage(), true))
        .onErrorResume(WebClientResponseException.class,
            ex -> this.downstreamUnavailable(movieId, ex))
        .onErrorResume(WebClientRequestException.class,
            ex -> this.downstreamUnreachable(movieId, ex))
        .doOnError(err -> log.warn("complete: movie={} terminó en error: {}", movieId,
            err.getMessage()));
  }

  private Mono<MovieDto> evaluateStatus(Long movieId, CompleteMovieRequest request,
      UploadStatusDto status) {
    String state = status.status() == null ? "" : status.status();
    log.debug("complete: movie={} estado storage={} key={}", movieId, state, status.storageKey());
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
        return Mono.error(new PendingUploadException());
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

  private Mono<MovieDto> persistReady(Long movieId, CompleteMovieRequest request,
      UploadStatusDto status) {
    log.info("complete: movie={} veredicto OK, persistiendo READY con object_key={}",
        movieId, status.storageKey());
    return this.moviesWebClient
        .completeMovie(movieId, this.toStorageId(status.uploadId()), status.storageKey())
        .doOnSuccess(movie -> log.info("complete: movie={} READY persistida", movieId))
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

  private Mono<MovieDto> onMoviesCompleteError(Long movieId, CompleteMovieRequest request,
      WebClientResponseException ex) {
    if (ex.getStatusCode().value() == 404) {
      log.warn("complete: movie={} no existe al persistir; rollback de objeto", movieId);
      return this.rollback(movieId, request, "MOVIE_MISSING",
          "La película no existe al momento del complete", false);
    }
    if (ex.getStatusCode().value() == 409) {
      return this.moviesWebClient
          .movieById(movieId)
          .flatMap(movie -> "READY".equals(movie.status())
              ? Mono.just(movie)
              : Mono.error(new UploadVerdictException("UPLOAD_CONFLICT",
                  "La película no pudo completarse: estado " + movie.status())))
          .switchIfEmpty(Mono.error(new UploadVerdictException("MOVIE_MISSING",
              "La película no existe al reconciliar el conflicto")));
    }
    return this.downstreamUnavailable(movieId, ex);
  }

  /** Rollback: elimina la película + el objeto (restaura cuota). Opcionalmente penaliza. */
  private Mono<MovieDto> rollback(Long movieId, CompleteMovieRequest request, String code,
      String reason, boolean penalize) {
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
  private Mono<MovieDto> downstreamUnavailable(Long movieId, WebClientResponseException ex) {
    log.error("complete: movie={} servicio aguas abajo no disponible: status={} {}",
        movieId, ex.getStatusCode(), ex.getMessage());
    return Mono.error(new UploadOrchestrationException(
        HttpStatus.valueOf(ex.getStatusCode().value()), "DOWNSTREAM_UNAVAILABLE",
        "Servicio aguas abajo no disponible: " + ex.getStatusCode()));
  }

  /** Servicio aguas abajo inalcanzable (conexión): sin rollback, el front puede reintentar. */
  private Mono<MovieDto> downstreamUnreachable(Long movieId, WebClientRequestException ex) {
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
