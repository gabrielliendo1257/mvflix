package com.guille.media.bff.experience.catalog.web;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.guille.media.bff.app.dto.MovieListItemDto;
import com.guille.media.bff.experience.catalog.application.CatalogQuery;
import com.guille.media.bff.experience.catalog.application.GetCatalog;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.reactive.server.WebTestClient;

import reactor.core.publisher.Flux;

class CatalogControllerTest {

  private final GetCatalog getCatalog = mock(GetCatalog.class);
  private WebTestClient client;

  @BeforeEach
  void setUp() {
    this.client = WebTestClient.bindToController(new CatalogController(this.getCatalog)).build();
  }

  @Test
  void returnsOwnedCatalogItems() {
    when(this.getCatalog.execute(CatalogQuery.withLimit(null)))
        .thenReturn(Flux.just(new MovieListItemDto(
            7L, "READY", "PRIVATE", "MOVIE", "Dune", 2021, "/poster.jpg")));

    this.client.get()
        .uri("/web/catalog")
        .exchange()
        .expectStatus().isOk()
        .expectBody()
        .jsonPath("$[0].id").isEqualTo(7)
        .jsonPath("$[0].title").isEqualTo("Dune")
        .jsonPath("$[0].poster_path").isEqualTo("/poster.jpg");
  }

  @Test
  void forwardsLimitParameter() {
    when(this.getCatalog.execute(CatalogQuery.withLimit(5))).thenReturn(Flux.empty());

    this.client.get()
        .uri("/web/catalog?limit=5")
        .exchange()
        .expectStatus().isOk()
        .expectBody().json("[]");

    org.mockito.Mockito.verify(this.getCatalog).execute(CatalogQuery.withLimit(5));
  }
}
