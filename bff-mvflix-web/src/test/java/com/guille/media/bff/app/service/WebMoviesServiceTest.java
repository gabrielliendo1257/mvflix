package com.guille.media.bff.app.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.guille.media.bff.app.dto.MovieDetailDto;
import com.guille.media.bff.app.dto.MediaAssetDto;
import com.guille.media.bff.app.dto.MovieDto;
import com.guille.media.bff.app.dto.StreamingSessionDto;
import com.guille.media.bff.app.ports.MoviesWebClient;
import com.guille.media.bff.app.ports.StorageWebClient;
import com.guille.media.bff.app.ports.UsersWebPort;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.List;

class WebMoviesServiceTest {

  private final MoviesWebClient moviesWebClient = mock(MoviesWebClient.class);
  private final StorageWebClient storageWebClient = mock(StorageWebClient.class);
  private final UsersWebPort usersWebPort = mock(UsersWebPort.class);
  private final StreamTicketService streamTicketService = new StreamTicketService("test-secret", 300);
  private WebMoviesService service;

  @BeforeEach
  void setUp() {
    this.service = new WebMoviesService(this.moviesWebClient, this.storageWebClient,
        this.usersWebPort, this.streamTicketService);
  }

  private static MovieDto movie(Long id, Long objectId) {
    return new MovieDto(id, "READY", objectId, "PRIVATE", "The Colossus of Rhodes", null, 1961,
        List.of("Adventure"), 3.2, "2h 7m", "Sergio Leone", List.of("Rory Calhoun"),
        "Overview...", null, "1961-06-15", "Italy", "Italian", null, "ENRICHED");
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
  void detailWithoutObjectIdTriesLocalPlaybackAndDegradesWhenNoAsset() {
    when(moviesWebClient.movieById(1L)).thenReturn(Mono.just(movie(1L, null)));
    when(moviesWebClient.assetByMovie(1L))
        .thenReturn(Mono.error(new RuntimeException("sin asset de biblioteca")));

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
  void libraryMoviePlaybackPointsToBffStreamProxy() {
    when(moviesWebClient.movieById(1L)).thenReturn(Mono.just(movie(1L, null)));
    when(moviesWebClient.assetByMovie(1L))
        .thenReturn(Mono.just(new com.guille.media.bff.app.dto.MediaAssetDto(
            5L, 7L, "Interstellar (2014).mkv", 100, "video/x-matroska", "IDENTIFIED", 1L)));

    StepVerifier.create(service.detail(1L))
        .assertNext(detail -> {
          assertThat(detail.playback().available()).isTrue();
          assertThat(detail.playback().url()).isEqualTo("/web/movies/1/stream");
        })
        .verifyComplete();

    verify(storageWebClient, never()).stream(org.mockito.ArgumentMatchers.anyString());
  }

  @Test
  void streamForwardsRangeToStorage() {
    when(moviesWebClient.assetByMovie(1L))
        .thenReturn(Mono.just(new com.guille.media.bff.app.dto.MediaAssetDto(
            5L, 7L, "Interstellar (2014).mkv", 100, "video/x-matroska", "IDENTIFIED", 1L)));
    when(storageWebClient.streamLibraryFile(eq(7L), eq("Interstellar (2014).mkv"), any()))
        .thenReturn(Mono.just(org.springframework.http.ResponseEntity.ok().build()));

    StepVerifier.create(service.stream(1L, "bytes=0-99", null))
        .expectNextCount(1)
        .verifyComplete();

    verify(storageWebClient).streamLibraryFile(7L, "Interstellar (2014).mkv", "bytes=0-99");
  }

  @Test
  void streamWithoutAssetIsNotFound() {
    when(moviesWebClient.assetByMovie(1L)).thenReturn(Mono.empty());

    StepVerifier.create(service.stream(1L, null, null))
        .expectNextMatches(response -> response.getStatusCode().is4xxClientError())
        .verifyComplete();
  }

  @Test
  void streamWithDeniedAccessIsForbidden() {
    when(moviesWebClient.assetByMovie(1L))
        .thenReturn(Mono.error(new WebClientResponseException(
            403, "Forbidden", org.springframework.http.HttpHeaders.EMPTY, null, null)));

    StepVerifier.create(service.stream(1L, null, null))
        .expectError(WebClientResponseException.class)
        .verify();
  }

  @Test
  void visibilityForwardsToMovies() {
    when(moviesWebClient.updateVisibility(1L, "PUBLIC"))
        .thenReturn(Mono.just(movie(1L, null)));

    StepVerifier.create(service.visibility(1L, "PUBLIC"))
        .expectNextCount(1)
        .verifyComplete();

    verify(moviesWebClient).updateVisibility(1L, "PUBLIC");
  }

  @Test
  void sharesForwardsToMovies() {
    when(moviesWebClient.updateShares(1L, List.of("Maria")))
        .thenReturn(Mono.just(movie(1L, null)));

    StepVerifier.create(service.shares(1L, List.of("Maria")))
        .expectNextCount(1)
        .verifyComplete();

    verify(moviesWebClient).updateShares(1L, List.of("Maria"));
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

  @Test
  void previewReturnsCandidateMetadataWithoutPersisting() {
    when(moviesWebClient.previewCandidate(43020L))
        .thenReturn(Mono.just(new com.guille.media.bff.app.dto.MovieEnrichmentPreviewDto(
            "The Colossus of Rhodes", "Il colosso di Rodi", 1961,
            List.of("Adventure"), 3.2, "2h 7m", "Sergio Leone", List.of("Rory Calhoun"),
            "Overview...", null, "1961-06-15", "Italy", "Italian", 43020L)));

    StepVerifier.create(service.preview(43020L))
        .assertNext(preview -> {
          assertThat(preview.title()).isEqualTo("The Colossus of Rhodes");
          assertThat(preview.tmdbId()).isEqualTo(43020L);
          assertThat(preview.director()).isEqualTo("Sergio Leone");
        })
        .verifyComplete();

    verify(moviesWebClient).previewCandidate(43020L);
  }

  @Test
  void streamWithValidTicketProxiesLibraryFile() {
    String ticket = this.streamTicketService.issue(7L, "user-jwt-value");
    when(moviesWebClient.assetByMovie(7L))
        .thenReturn(Mono.just(new MediaAssetDto(1L, 3L, "Blade Runner 2049.mkv",
            2097152, "video/x-matroska", "IDENTIFIED", 7L)));
    when(storageWebClient.streamLibraryFile(3L, "Blade Runner 2049.mkv", "bytes=0-1023"))
        .thenReturn(Mono.just(org.springframework.http.ResponseEntity.<org.springframework.core.io.buffer.DataBuffer>status(206).build()));

    StepVerifier.create(service.stream(7L, "bytes=0-1023", ticket))
        .assertNext(response -> assertThat(response.getStatusCode().value()).isEqualTo(206))
        .verifyComplete();
  }

  @Test
  void streamWithTicketOfAnotherMovieIsRejected() {
    String ticket = this.streamTicketService.issue(7L, "user-jwt-value");

    StepVerifier.create(service.stream(8L, "bytes=0-1023", ticket))
        .expectError(StreamTicketException.class)
        .verify();
  }

  @Test
  void streamWithExpiredTicketIsRejected() {
    StreamTicketService shortLived = new StreamTicketService("test-secret", -1);
    String ticket = shortLived.issue(7L, "user-jwt-value");

    StepVerifier.create(service.stream(7L, "bytes=0-1023", ticket))
        .expectError(StreamTicketException.class)
        .verify();
  }

  @Test
  void streamWithoutTicketUsesSecurityContext() {
    when(moviesWebClient.assetByMovie(7L))
        .thenReturn(Mono.just(new MediaAssetDto(1L, 3L, "Blade Runner 2049.mkv",
            2097152, "video/x-matroska", "IDENTIFIED", 7L)));
    when(storageWebClient.streamLibraryFile(3L, "Blade Runner 2049.mkv", "bytes=0-1023"))
        .thenReturn(Mono.just(org.springframework.http.ResponseEntity.<org.springframework.core.io.buffer.DataBuffer>status(206).build()));

    StepVerifier.create(service.stream(7L, "bytes=0-1023", null))
        .assertNext(response -> assertThat(response.getStatusCode().value()).isEqualTo(206))
        .verifyComplete();
  }
}