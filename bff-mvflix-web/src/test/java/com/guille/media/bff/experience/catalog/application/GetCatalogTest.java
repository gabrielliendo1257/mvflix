package com.guille.media.bff.experience.catalog.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.guille.media.bff.app.dto.MovieDto;
import com.guille.media.bff.app.ports.MoviesWebClient;

import org.junit.jupiter.api.Test;

import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.util.List;

class GetCatalogTest {

  private final MoviesWebClient movies = mock(MoviesWebClient.class);
  private final GetCatalog getCatalog = new GetCatalog(this.movies);

  private MovieDto movie(long id) {
    return new MovieDto(id, "READY", null, "PRIVATE", "MOVIE", "Dune", null,
        2021, List.of(), null, null, null, List.of(), "/poster.jpg", null,
        null, null, null, null, null);
  }

  @Test
  void mapsOwnedMoviesToGridItems() {
    when(this.movies.listOwnedMovies(CatalogQuery.DEFAULT_LIMIT))
        .thenReturn(Flux.just(movie(7L), movie(8L)));

    StepVerifier.create(this.getCatalog.execute(CatalogQuery.withLimit(null)))
        .assertNext(item -> {
          assertThat(item.id()).isEqualTo(7L);
          assertThat(item.title()).isEqualTo("Dune");
          assertThat(item.visibility()).isEqualTo("PRIVATE");
        })
        .assertNext(item -> assertThat(item.id()).isEqualTo(8L))
        .verifyComplete();
  }

  @Test
  void capsAndNormalizesTheLimit() {
    when(this.movies.listOwnedMovies(CatalogQuery.MAX_LIMIT)).thenReturn(Flux.empty());

    StepVerifier.create(this.getCatalog.execute(CatalogQuery.withLimit(5000)))
        .verifyComplete();

    org.mockito.Mockito.verify(this.movies).listOwnedMovies(CatalogQuery.MAX_LIMIT);
  }

  @Test
  void nonPositiveLimitFallsBackToDefault() {
    when(this.movies.listOwnedMovies(CatalogQuery.DEFAULT_LIMIT)).thenReturn(Flux.empty());

    this.getCatalog.execute(CatalogQuery.withLimit(0)).collectList().block();
    this.getCatalog.execute(CatalogQuery.withLimit(-3)).collectList().block();

    org.mockito.Mockito.verify(this.movies,
            org.mockito.Mockito.times(2))
        .listOwnedMovies(CatalogQuery.DEFAULT_LIMIT);
  }
}
