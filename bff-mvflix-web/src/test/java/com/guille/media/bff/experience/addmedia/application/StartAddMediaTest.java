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
import com.guille.media.bff.app.dto.UserProfile;
import com.guille.media.bff.app.ports.UsersWebPort;
import com.guille.media.bff.experience.addmedia.application.port.AddMediaMovies;
import com.guille.media.bff.experience.addmedia.application.port.AddMediaStorage;
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

  private final AddMediaMovies movies = mock(AddMediaMovies.class);
  private final AddMediaStorage storage = mock(AddMediaStorage.class);
  private final UsersWebPort users = mock(UsersWebPort.class);
  private final InMemoryAddMediaProcessRepository processes =
      new InMemoryAddMediaProcessRepository();

  private StartAddMedia useCase;

  @BeforeEach
  void setUp() {
    when(this.users.me()).thenReturn(Mono.just(new UserProfile(
        "u1", "pepe", "pepe@mvflix.dev", "FREE", true, 0, false)));
    this.useCase = new StartAddMedia(this.movies, this.storage, this.processes, this.users);
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
    when(this.movies.createDraft(any())).thenReturn(Mono.just(draft()));
    when(this.storage.prepareUpload(any(UploadCreateRequest.class)))
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

    verify(this.movies).createDraft(any());
    verify(this.storage).prepareUpload(any(UploadCreateRequest.class));
    verify(this.movies, never()).discardDraft(any());
  }

  @Test
  void replayWithSameIdempotencyKeyReturnsSameProcessWithoutSideEffects() {
    when(this.movies.createDraft(any())).thenReturn(Mono.just(draft()));
    when(this.storage.prepareUpload(any(UploadCreateRequest.class)))
        .thenReturn(Mono.just(session()));
    // El replay renueva las instrucciones en vez de devolver upload=null.
    when(this.storage.refreshInstructions(42L))
        .thenReturn(Mono.just(new UploadSessionDto("42", "http://minio/fresh",
            "pepe/videos/a.mp4", "PUT", "PENDING",
            new UploadSessionDto.ExpectedObjectData(1024L, "video/mp4"))));

    AddMediaView first = start().block();
    AddMediaView replay = start().block();

    assertThat(replay.addMediaId()).isEqualTo(first.addMediaId());
    assertThat(replay.uploadId()).isEqualTo(42L);
    assertThat(replay.phase()).isEqualTo(AddMediaPhase.WAITING_FOR_UPLOAD);
    assertThat(replay.upload().url()).isEqualTo("http://minio/fresh");
    // Un solo draft y una sola sesión de upload para el mismo intento.
    verify(this.movies, org.mockito.Mockito.times(1)).createDraft(any());
    verify(this.storage, org.mockito.Mockito.times(1)).prepareUpload(any());
  }

  @Test
  void concurrentStartsWithSameKeyCreateSideEffectsOnlyOnce() {
    when(this.movies.createDraft(any())).thenAnswer(
        inv -> Mono.delay(java.time.Duration.ofMillis(50)).thenReturn(draft()));
    when(this.storage.prepareUpload(any(UploadCreateRequest.class)))
        .thenReturn(Mono.just(session()));

    Mono<AddMediaView> v1 = start();
    Mono<AddMediaView> v2 = start();
    StepVerifier.create(Mono.zip(v1, v2))
        .assertNext(tuple -> {
          // Uno gana y termina WAITING_FOR_UPLOAD; el otro ve PREPARING o
          // WAITING_FOR_UPLOAD según el timing de la persistencia final.
          assertThat(tuple.getT1().phase()).isIn(
              AddMediaPhase.WAITING_FOR_UPLOAD, AddMediaPhase.PREPARING);
          assertThat(tuple.getT2().phase()).isIn(
              AddMediaPhase.WAITING_FOR_UPLOAD, AddMediaPhase.PREPARING);
        })
        .verifyComplete();

    verify(this.movies, org.mockito.Mockito.times(1)).createDraft(any());
    verify(this.storage, org.mockito.Mockito.times(1)).prepareUpload(any());
  }

  @Test
  void loserOfTheClaimSeesPreparingWithoutSideEffects() {
    // Proceso existente ya reclamado por otro request en vuelo.
    this.processes.createIfAbsent("pepe", "intent-1").block();
    String id = this.processes.createIfAbsent("pepe", "intent-1").block().id().value();
    org.assertj.core.api.Assertions.assertThat(
        this.processes.tryClaim(com.guille.media.bff.experience.addmedia.model.AddMediaId
            .parse(id)).block()).isTrue();

    StepVerifier.create(start())
        .assertNext(view -> {
          assertThat(view.phase()).isEqualTo(AddMediaPhase.PREPARING);
          assertThat(view.movieId()).isNull();
        })
        .verifyComplete();

    verify(this.movies, never()).createDraft(any());
    verify(this.storage, never()).prepareUpload(any());
  }

  @Test
  void storageFailureDiscardsOnlyThisProcessDraftAndPropagates() {
    when(this.movies.createDraft(any())).thenReturn(Mono.just(draft()));
    when(this.movies.discardDraft(7L)).thenReturn(Mono.empty());
    when(this.storage.prepareUpload(any(UploadCreateRequest.class)))
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
    when(this.users.me()).thenReturn(Mono.just(new UserProfile(
        "u1", "pepe", "pepe@mvflix.dev", "FREE", true, 5, true)));

    StepVerifier.create(start())
        .expectErrorSatisfies(error -> {
          com.guille.media.bff.experience.addmedia.application.UploadOrchestrationException ex =
              (com.guille.media.bff.experience.addmedia.application.
                  UploadOrchestrationException) error;
            assertThat(ex.getCode()).isEqualTo("USER_BLOCKED");
          })
        .verify();

    verify(this.storage, never()).prepareUpload(any());
    verify(this.movies, never()).discardDraft(any());
  }
}
