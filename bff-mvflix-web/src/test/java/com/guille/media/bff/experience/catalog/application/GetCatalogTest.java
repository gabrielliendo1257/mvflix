package com.guille.media.bff.experience.catalog.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.guille.media.bff.app.ports.MoviesWebClient;

import org.junit.jupiter.api.Test;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.List;

class GetCatalogTest {

  private final MoviesWebClient movies = mock(MoviesWebClient.class);
  private final GetCatalog getCatalog = new GetCatalog(this.movies);

  private CatalogPage pageWith(String title, String source, String status) {
    return new CatalogPage(
        new CatalogPage.Summary(1, 1, 0),
        List.of(new CatalogPage.Item(
            new CatalogPage.Key("MEDIA", 42L), 42L, null, title, "/p.jpg", 2009,
            "1h 40m", "MOVIE", status, "READY".equals(status) ? "READY" : "PROCESSING",
            source, "PRIVATE", 2, "LINKED")),
        0, 25, 1, 1);
  }

  @Test
  void returnsProjectionFromMovies() {
    when(this.movies.catalogPage(0, 25, null, null, null, null))
        .thenReturn(Mono.just(pageWith("Coraline", "MANAGED", "READY")));

    StepVerifier.create(this.getCatalog.execute(null, null, null, null, null, null))
        .assertNext(page -> {
          assertThat(page.total()).isEqualTo(1);
          assertThat(page.summary().ready()).isEqualTo(1);
          var item = page.items().get(0);
          assertThat(item.title()).isEqualTo("Coraline");
          assertThat(item.source()).isEqualTo("MANAGED");
          assertThat(item.key().type()).isEqualTo("MEDIA");
          assertThat(item.playable()).isTrue();
        })
        .verifyComplete();
  }

  @Test
  void forwardsNormalizedFilters() {
    when(this.movies.catalogPage(2, 10, "cora", "DRAFT", "title", "ASC"))
        .thenReturn(Mono.just(CatalogPage.empty()));

    this.getCatalog.execute(2, 10, "  cora ", " DRAFT ", " title ", "ASC").block();

    verify(this.movies).catalogPage(2, 10, "cora", "DRAFT", "title", "ASC");
  }

  @Test
  void blankParametersAreSentAsNullAndSizeCappedAtMax() {
    when(this.movies.catalogPage(0, CatalogQuery.MAX_LIMIT, null, null, null, null))
        .thenReturn(Mono.just(CatalogPage.empty()));

    this.getCatalog.execute(-1, 500, "   ", "", null, null).block();

    verify(this.movies)
        .catalogPage(0, CatalogQuery.MAX_LIMIT, null, null, null, null);
  }
}
