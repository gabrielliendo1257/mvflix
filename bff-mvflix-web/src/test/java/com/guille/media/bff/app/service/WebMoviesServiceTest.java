package com.guille.media.bff.app.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.guille.media.bff.app.dto.MovieDetailDto;
import com.guille.media.bff.app.dto.MovieDto;
import com.guille.media.bff.app.dto.StreamingSessionDto;
import com.guille.media.bff.app.ports.MoviesWebClient;
import com.guille.media.bff.app.ports.StorageWebClient;
import com.guille.media.bff.app.ports.UsersWebPort;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClientRequestException;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.List;

class WebMoviesServiceTest {

  private final MoviesWebClient moviesWebClient = mock(MoviesWebClient.class);
  private final StorageWebClient storageWebClient = mock(StorageWebClient.class);
  private final UsersWebPort usersWebPort = mock(UsersWebPort.class);
  private WebMoviesService service;

  @BeforeEach
  void setUp() {
    this.service = new WebMoviesService(this.moviesWebClient, this.storageWebClient,
        this.usersWebPort);
  }

  private static MovieDto movie(Long id, Long objectId) {
    return new MovieDto(id, "READY", objectId, "The Colossus of Rhodes", null, 1961,
        List.of("Adventure"), 3.2, "2h 7m", "Sergio Leone", List.of("Rory Calhoun"),
        "Overview...", null, "1961-06-15", "Italy", "Italian", null);
  }

  @Test
  void detailWithObjectIdReturnsPlaybackUrl() {
    when(moviesWebClient.movieById(1L)).thenReturn(Mono.just(movie(1L, 42L)));
    when(storageWebClient.stream("42"))
        .thenReturn(Mono.just(new StreamingSessionDto("42", "http://minio/42?sig=abc",
            null, "2026-01-01T00:00:00Z", "GET")));

    StepVerifier.create(service.detail(1L))
        .assertNext(detail -> {
          assertThat(detail.movie().id()).isEqualTo(1L);
          assertThat(detail.playback().available()).isTrue();
          assertThat(detail.playback().url()).isEqualTo("http://minio/42?sig=abc");
        })
        .verifyComplete();

    verify(storageWebClient).stream("42");
  }

  @Test
  void detailWithoutObjectIdIsUnavailableWithoutCallingStorage() {
    when(moviesWebClient.movieById(1L)).thenReturn(Mono.just(movie(1L, null)));

    StepVerifier.create(service.detail(1L))
        .assertNext(detail -> {
          assertThat(detail.movie().id()).isEqualTo(1L);
          assertThat(detail.playback().available()).isFalse();
          assertThat(detail.playback().url()).isNull();
        })
        .verifyComplete();

    verify(storageWebClient, never()).stream(org.mockito.ArgumentMatchers.anyString());
  }

  @Test
  void detailDegradesGracefullyWhenStorageIsDown() {
    when(moviesWebClient.movieById(1L)).thenReturn(Mono.just(movie(1L, 42L)));
    when(storageWebClient.stream("42"))
        .thenReturn(Mono.error(new WebClientRequestException(
            new IllegalStateException("conexión rechazada"),
            org.springframework.http.HttpMethod.POST,
            java.net.URI.create("http://storage/streaming"),
            org.springframework.http.HttpHeaders.EMPTY)));

    StepVerifier.create(service.detail(1L))
        .assertNext(detail -> {
          assertThat(detail.movie().id()).isEqualTo(1L);
          assertThat(detail.playback().available()).isFalse();
          assertThat(detail.playback().url()).isNull();
        })
        .verifyComplete();
  }
}