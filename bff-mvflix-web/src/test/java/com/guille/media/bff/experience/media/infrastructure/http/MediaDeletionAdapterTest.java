package com.guille.media.bff.experience.media.infrastructure.http;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.guille.media.bff.app.ports.MoviesWebClient;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

/**
 * La idempotencia vive aquí: el 404 de movies ("no existe" o "no es tuya",
 * sin revelar existencia) se traduce a {@code false}, nunca a error.
 */
class MediaDeletionAdapterTest {

  private final MoviesWebClient movies = mock(MoviesWebClient.class);
  private final MediaDeletionAdapter adapter = new MediaDeletionAdapter(this.movies);

  @Test
  void successMapsToTrue() {
    when(this.movies.deleteMovie(42L)).thenReturn(Mono.empty());

    StepVerifier.create(this.adapter.deleteCatalog(42L))
        .assertNext(assertThat(Boolean.TRUE)::isEqualTo)
        .verifyComplete();
  }

  @Test
  void notFoundMapsToFalseIdempotently() {
    when(this.movies.deleteMovie(42L))
        .thenReturn(Mono.error(WebClientResponseException.create(
            HttpStatus.NOT_FOUND.value(), "Not Found", org.springframework.http.HttpHeaders.EMPTY,
            new byte[0], null)));

    StepVerifier.create(this.adapter.deleteCatalog(42L))
        .assertNext(assertThat(Boolean.FALSE)::isEqualTo)
        .verifyComplete();
  }

  @Test
  void otherErrorsPropagate() {
    when(this.movies.deleteMovie(42L))
        .thenReturn(Mono.error(WebClientResponseException.create(
            HttpStatus.INTERNAL_SERVER_ERROR.value(), "Server Error",
            org.springframework.http.HttpHeaders.EMPTY, new byte[0], null)));

    StepVerifier.create(this.adapter.deleteCatalog(42L))
        .expectError(WebClientResponseException.class)
        .verify();
  }
}
