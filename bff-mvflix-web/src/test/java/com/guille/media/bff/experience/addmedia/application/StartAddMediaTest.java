package com.guille.media.bff.experience.addmedia.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.guille.media.bff.app.dto.CreateMovieRequest;
import com.guille.media.bff.app.dto.MovieDto;
import com.guille.media.bff.app.dto.UploadCreateRequest;
import com.guille.media.bff.app.dto.UploadSessionDto;
import com.guille.media.bff.app.ports.StorageWebClient;
import com.guille.media.bff.app.service.WebMoviesService;
import com.guille.media.bff.experience.addmedia.application.port.AddMediaProcessRepository;
import com.guille.media.bff.experience.addmedia.model.AddMediaPhase;
import com.guille.media.bff.experience.addmedia.web.AddMediaView;
import com.guille.media.bff.experience.addmedia.web.StartAddMediaRequest;
import com.guille.media.bff.infrastructure.persistence.InMemoryAddMediaProcessRepository;
import com.guille.media.bff.experience.addmedia.application.UploadCompletionOutcome;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.List;

class StartAddMediaTest {

  private final WebMoviesService movies = mock(WebMoviesService.class);
  private final StorageWebClient storage = mock(StorageWebClient.class);
  private final InMemoryAddMediaProcessRepository processes =
      new InMemoryAddMediaProcessRepository();

  private StartAddMedia useCase;

  @BeforeEach
  void setUp() {
    this.useCase = new StartAddMedia(this.movies, this.storage, this.processes);
  }

  private StartAddMediaRequest request(String idempotencyKey) {
    return new StartAddMediaRequest(
        new StartAddMediaRequest.FileSelection("alien.mp4", 1024L, "video/mp4"),
        new StartAddMediaRequest.MovieSelection(
            348L, new CreateMovieRequest("Alien", null, 1979, List.of(), null, null,
                null, List.of(), null, null, null, null, null, null, "MOVIE")),
        new StartAddMediaRequest.InitialAccess("PRIVATE", List.of()),
        idempotencyKey);
  }

  private UploadSessionDto session() {
    return new UploadSessionDto("42", "http://minio/put", "pepe/videos/a.mp4", "PUT",
        "PENDING", new UploadSessionDto.ExpectedObjectData(1024L, "video/mp4"));
  }

  private MovieDto draft() {
    return new MovieDto(7L, "DRAFT", null, "PRIVATE", "MOVIE", "Alien", null, 1979,
        List.of(), null, null, null, List.of(), null, null, null, null, null, null, null);
  }

  private Mono<AddMediaView> start() {
    return this.useCase.handle("pepe", request("intent-1"));
  }

  @Test
  void happyPathCreatesDraftPreparesUploadAndPersistsWaitingForUpload() {
    when(this.movies.create(any())).thenReturn(Mono.just(draft()));
    when(this.storage.createUpload(any(UploadCreateRequest.class)))
        .thenReturn(Mono.just(session()));

    StepVerifier.create(start())
        .assertNext(view -> {
          assertThat(view.phase()).isEqualTo(AddMediaPhase.WAITING_FOR_UPLOAD);
          assertThat(view.movieId()).isEqualTo(7L);
          assertThat(view.uploadId()).isEqualTo(42L);
          assertThat(view.upload().url()).isEqualTo("http://minio/put");
          assertThat(view.upload().method()).isEqualTo("PUT");
          assertThat(view.upload().expectedSizeBytes()).isEqualTo(1024L);
        })
        .verifyComplete();

    verify(this.movies).create(any());
    verify(this.storage).createUpload(any(UploadCreateRequest.class));
    verify(this.movies, never()).discardDraft(any());
  }

  @Test
  void replayWithSameIdempotencyKeyReturnsSameProcessWithoutSideEffects() {
    when(this.movies.create(any())).thenReturn(Mono.just(draft()));
    when(this.storage.createUpload(any(UploadCreateRequest.class)))
        .thenReturn(Mono.just(session()));

    AddMediaView first = start().block();
    AddMediaView replay = start().block();

    assertThat(replay.addMediaId()).isEqualTo(first.addMediaId());
    assertThat(replay.uploadId()).isEqualTo(42L);
    // Un solo draft y una sola sesión de upload para el mismo intento.
    verify(this.movies, org.mockito.Mockito.times(1)).create(any());
    verify(this.storage, org.mockito.Mockito.times(1)).createUpload(any());
  }

  @Test
  void storageFailureDiscardsOnlyThisProcessDraftAndPropagates() {
    when(this.movies.create(any())).thenReturn(Mono.just(draft()));
    when(this.movies.discardDraft(7L)).thenReturn(Mono.empty());
    when(this.storage.createUpload(any(UploadCreateRequest.class)))
        .thenReturn(Mono.error(new RuntimeException("storage unavailable")));

    StepVerifier.create(start())
        .expectErrorMessage("storage unavailable")
        .verify();

    // Compensación acotada: solo el draft de este intento.
    verify(this.movies).discardDraft(7L);
    // El proceso queda en STARTING: un replay con la misma key reintenta limpio.
    this.processes.createIfAbsent("pepe", "intent-1")
        .as(StepVerifier::create)
        .assertNext(process -> {
          assertThat(process.phase()).isEqualTo(AddMediaPhase.STARTING);
          assertThat(process.movieId()).isNull();
        })
        .verifyComplete();
  }

  @Test
  void blockedUserSurfacesForbiddenWithoutPreparingUpload() {
    when(this.movies.create(any()))
        .thenReturn(Mono.error(new com.guille.media.bff.experience.addmedia.application.
            UploadOrchestrationException(HttpStatus.FORBIDDEN, "USER_BLOCKED", "bloqueado")));

    StepVerifier.create(start())
        .expectErrorSatisfies(error -> {
          com.guille.media.bff.experience.addmedia.application.UploadOrchestrationException ex =
              (com.guille.media.bff.experience.addmedia.application.
                  UploadOrchestrationException) error;
            assertThat(ex.getCode()).isEqualTo("USER_BLOCKED");
          })
        .verify();

    verify(this.storage, never()).createUpload(any());
    verify(this.movies, never()).discardDraft(any());
  }
}
