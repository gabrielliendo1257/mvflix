package com.guille.media.bff.app.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.guille.media.bff.app.dto.CompleteMovieRequest;
import com.guille.media.bff.app.dto.MovieDto;
import com.guille.media.bff.app.dto.UploadStatusDto;
import com.guille.media.bff.app.ports.MoviesWebClient;
import com.guille.media.bff.app.ports.StorageWebClient;
import com.guille.media.bff.app.ports.UsersWebPort;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

/**
 * Tests de caracterización del flujo completo de alta (POST /web/movies/{id}/complete).
 * Fijan el comportamiento ACTUAL -incluidos el retry de PENDING x3 con rollback y la
 * penalidad por veredicto- para que el refactor posterior a la experiencia Add Media
 * pueda demostrar qué cambia y qué se conserva.
 */
class WebMoviesServiceCompleteFlowTest {

  private final MoviesWebClient moviesWebClient = mock(MoviesWebClient.class);
  private final StorageWebClient storageWebClient = mock(StorageWebClient.class);
  private final UsersWebPort usersWebPort = mock(UsersWebPort.class);

  private WebMoviesService service;

  @BeforeEach
  void setUp() {
    when(this.usersWebPort.reportViolation(anyString())).thenReturn(Mono.empty());
    this.service =
        new WebMoviesService(
            this.moviesWebClient,
            this.storageWebClient,
            this.usersWebPort,
            new StreamTicketService("test-secret", 300),
            new JobStore(),
            new AddMediaCompletion(this.moviesWebClient, this.storageWebClient,
                this.usersWebPort));
  }

  private static MovieDto movie(Long id, String status) {
    return new MovieDto(id, status, null, "PRIVATE", "MOVIE", "The Colossus of Rhodes", null, 1961,
        java.util.List.of("Adventure"), 3.2, "2h 7m", "Sergio Leone",
        java.util.List.of("Rory Calhoun"), "Overview...", null, "1961-06-15", "Italy",
        "Italian", null, "ENRICHED");
  }

  private static UploadStatusDto status(String status, long expectedSize) {
    return new UploadStatusDto(
        "42", "pepe/videos/a.mp4", status,
        new UploadStatusDto.ExpectedObjectData(expectedSize, "video/mp4"));
  }

  private void stubDraftMovie() {
    when(this.moviesWebClient.movieById(7L)).thenReturn(Mono.just(movie(7L, "DRAFT")));
  }

  @Test
  void completeIsIdempotentWhenMovieAlreadyReady() {
    MovieDto ready = movie(7L, "READY");
    when(this.moviesWebClient.movieById(7L)).thenReturn(Mono.just(ready));

    StepVerifier.create(this.service.complete(7L, new CompleteMovieRequest(42L, 1024L)))
        .assertNext(outcome -> {
          assertThat(((UploadCompletionOutcome.Completed) outcome).movie()).isEqualTo(ready);
        })
        .verifyComplete();

    verifyNoStorageInteractions();
  }

  @Test
  void completedUploadPersistsMovieAsReady() {
    this.stubDraftMovie();
    when(this.storageWebClient.uploadStatus(42L))
        .thenReturn(Mono.just(status("COMPLETED", 1024L)));
    MovieDto ready = movie(7L, "READY");
    when(this.moviesWebClient.completeMovie(7L, 42L, "pepe/videos/a.mp4"))
        .thenReturn(Mono.just(ready));

    StepVerifier.create(this.service.complete(7L, new CompleteMovieRequest(42L, 1024L)))
        .assertNext(outcome -> {
          assertThat(outcome).isInstanceOf(UploadCompletionOutcome.Completed.class);
          assertThat(((UploadCompletionOutcome.Completed) outcome).movie()).isEqualTo(ready);
        })
        .verifyComplete();

    verify(this.moviesWebClient).completeMovie(7L, 42L, "pepe/videos/a.mp4");
    verify(this.usersWebPort, never()).reportViolation(anyString());
  }

  @Test
  void sizeMismatchRollsBackAndPenalizes() {
    this.stubDraftMovie();
    when(this.storageWebClient.uploadStatus(42L))
        .thenReturn(Mono.just(status("COMPLETED", 9999L)));
    when(this.moviesWebClient.deleteMovie(7L)).thenReturn(Mono.empty());
    when(this.storageWebClient.deleteObject(42L)).thenReturn(Mono.empty());

    StepVerifier.create(this.service.complete(7L, new CompleteMovieRequest(42L, 1024L)))
        .expectErrorSatisfies(error -> {
          assertThat(error).isInstanceOf(UploadOrchestrationException.class);
          assertThat(((UploadOrchestrationException) error).getCode())
              .isEqualTo("UPLOAD_INCONSISTENT");
          assertThat(((UploadOrchestrationException) error).getStatus())
              .isEqualTo(HttpStatus.CONFLICT);
        })
        .verify();

    verify(this.moviesWebClient).deleteMovie(7L);
    verify(this.storageWebClient).deleteObject(42L);
    verify(this.usersWebPort).reportViolation(anyString());
    verify(this.moviesWebClient, never()).completeMovie(anyLong(), anyLong(), anyString());
  }

  /**
   * CARACTERIZACIÓN DE UN DEFECTO ACTUAL: {@code retryWhen} envuelve el último
   * PendingUploadException en RetryExhaustedException, que ya no matchea el
   * onErrorResume(PendingUploadException) del servicio. Resultado real hoy:
   * escapa un error crudo (el handler global responde 500), NO se ejecuta el
   * rollback pensado para UPLOAD_PENDING y el front no distingue una demora
   * del webhook de un fallo definitivo. El commit que convierte PENDING en
   * verificación asíncrona (202) reemplazará este comportamiento.
   */
  /**
   * COMPORTAMIENTO NUEVO (reemplaza al defecto caracterizado antes): PENDING
   * es verificación asíncrona. Un solo sondeo, sin reintentos, sin rollback y
   * sin penalidad; el resultado StillVerifying se traduce en HTTP 202 para que
   * el front consulte de nuevo.
   */
  @Test
  void pendingUploadReturnsVerifyingWithoutAnyRollback() {
    this.stubDraftMovie();
    java.util.concurrent.atomic.AtomicInteger polls =
        new java.util.concurrent.atomic.AtomicInteger();
    when(this.storageWebClient.uploadStatus(42L))
        .thenAnswer(
            inv ->
                Mono.just(status("PENDING", 1024L))
                    .doOnSubscribe(sub -> polls.incrementAndGet()));

    StepVerifier.create(this.service.complete(7L, new CompleteMovieRequest(42L, 1024L)))
        .assertNext(outcome -> {
          assertThat(outcome).isInstanceOf(UploadCompletionOutcome.StillVerifying.class);
          assertThat(((UploadCompletionOutcome.StillVerifying) outcome).uploadId())
              .isEqualTo(42L);
        })
        .verifyComplete();

    assertThat(polls.get()).isEqualTo(1);
    // Ni borrado de draft, ni borrado de objeto, ni violación por una espera.
    verify(this.moviesWebClient, never()).deleteMovie(anyLong());
    verify(this.storageWebClient, never()).deleteObject(anyLong());
    verify(this.usersWebPort, never()).reportViolation(anyString());
  }

  @Test
  void failedVerdictRollsBackAndPenalizes() {
    this.stubDraftMovie();
    when(this.storageWebClient.uploadStatus(42L)).thenReturn(Mono.just(status("FAILED", 1024L)));
    when(this.moviesWebClient.deleteMovie(7L)).thenReturn(Mono.empty());
    when(this.storageWebClient.deleteObject(42L)).thenReturn(Mono.empty());

    StepVerifier.create(this.service.complete(7L, new CompleteMovieRequest(42L, 1024L)))
        .expectErrorSatisfies(error -> {
          assertThat(((UploadOrchestrationException) error).getCode()).isEqualTo("UPLOAD_FAILED");
        })
        .verify();

    verify(this.moviesWebClient).deleteMovie(7L);
    verify(this.storageWebClient).deleteObject(42L);
    verify(this.usersWebPort).reportViolation(anyString());
  }

  @Test
  void moviesMissingDuringCompleteRollsBackWithoutPenalty() {
    this.stubDraftMovie();
    when(this.storageWebClient.uploadStatus(42L))
        .thenReturn(Mono.just(status("COMPLETED", 1024L)));
    when(this.moviesWebClient.completeMovie(7L, 42L, "pepe/videos/a.mp4"))
        .thenReturn(Mono.error(WebClientResponseException.create(404, "nf", null, null, null)));
    when(this.moviesWebClient.deleteMovie(7L)).thenReturn(Mono.empty());
    when(this.storageWebClient.deleteObject(42L)).thenReturn(Mono.empty());

    StepVerifier.create(this.service.complete(7L, new CompleteMovieRequest(42L, 1024L)))
        .expectErrorSatisfies(error -> {
          assertThat(((UploadOrchestrationException) error).getCode()).isEqualTo("MOVIE_MISSING");
        })
        .verify();

    verify(this.moviesWebClient).deleteMovie(7L);
    verify(this.storageWebClient).deleteObject(42L);
    verify(this.usersWebPort, never()).reportViolation(anyString());
  }

  @Test
  void moviesConflictReconcilesWhenMovieAlreadyReady() {
    this.stubDraftMovie();
    when(this.storageWebClient.uploadStatus(42L))
        .thenReturn(Mono.just(status("COMPLETED", 1024L)));
    when(this.moviesWebClient.completeMovie(7L, 42L, "pepe/videos/a.mp4"))
        .thenReturn(Mono.error(WebClientResponseException.create(409, "cf", null, null, null)));
    // La reconciliación descubre que otro camino ya completó la película.
    MovieDto ready = movie(7L, "READY");
    when(this.moviesWebClient.movieById(7L)).thenReturn(Mono.just(ready));

    StepVerifier.create(this.service.complete(7L, new CompleteMovieRequest(42L, 1024L)))
        .assertNext(outcome -> {
          assertThat(((UploadCompletionOutcome.Completed) outcome).movie()).isEqualTo(ready);
        })
        .verifyComplete();

    verify(this.moviesWebClient, never()).deleteMovie(anyLong());
    verify(this.storageWebClient, never()).deleteObject(anyLong());
  }

  @Test
  void storageDownstreamFailureDoesNotRollBack() {
    this.stubDraftMovie();
    when(this.storageWebClient.uploadStatus(42L))
        .thenReturn(Mono.error(WebClientResponseException.create(503, "su", null, null, null)));

    StepVerifier.create(this.service.complete(7L, new CompleteMovieRequest(42L, 1024L)))
        .expectErrorSatisfies(error -> {
          assertThat(error).isInstanceOf(UploadOrchestrationException.class);
          assertThat(((UploadOrchestrationException) error).getCode())
              .isEqualTo("DOWNSTREAM_UNAVAILABLE");
        })
        .verify();

    // Un fallo transitorio del下游 no autoriza borrar el draft ni el objeto.
    verify(this.moviesWebClient, never()).deleteMovie(anyLong());
    verify(this.storageWebClient, never()).deleteObject(anyLong());
    verify(this.usersWebPort, never()).reportViolation(anyString());
  }

  private void verifyNoStorageInteractions() {
    verify(this.storageWebClient, never()).uploadStatus(anyLong());
    verify(this.moviesWebClient, never()).completeMovie(anyLong(), anyLong(), anyString());
  }
}
