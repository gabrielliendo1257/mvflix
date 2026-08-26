package com.guille.media.bff.experience.catalog.web;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.guille.media.bff.experience.catalog.application.CatalogPage;
import com.guille.media.bff.experience.catalog.application.GetCatalog;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.reactive.server.WebTestClient;

import reactor.core.publisher.Mono;

import java.util.List;

class CatalogControllerTest {

  private final GetCatalog getCatalog = mock(GetCatalog.class);
  private WebTestClient client;

  @BeforeEach
  void setUp() {
    this.client = WebTestClient.bindToController(new CatalogController(this.getCatalog)).build();
  }

  @Test
  void returnsOwnedCatalogPageContract() {
    when(this.getCatalog.execute(null, null, null, null, null, null))
        .thenReturn(Mono.just(new CatalogPage(
            new CatalogPage.Summary(128, 121, 7),
            List.of(new CatalogPage.Item(
                new CatalogPage.Key("MEDIA", 42L), 42L, 17L,
                "Coraline", "/poster.jpg", 2009, "1h 40m", "MOVIE",
                "READY", "READY", "LOCAL", "PRIVATE", 0, "LINKED")),
            0, 25, 128, 6)));

    this.client.get()
        .uri("/web/catalog")
        .exchange()
        .expectStatus().isOk()
        .expectBody()
        .jsonPath("$.summary.total").isEqualTo(128)
        .jsonPath("$.summary.needsAttention").isEqualTo(7)
        .jsonPath("$.items[0].key.type").isEqualTo("MEDIA")
        .jsonPath("$.items[0].key.id").isEqualTo(42)
        .jsonPath("$.items[0].mediaId").isEqualTo(42)
        .jsonPath("$.items[0].assetId").isEqualTo(17)
        .jsonPath("$.items[0].source").isEqualTo("LOCAL")
        .jsonPath("$.items[0].sharedWithCount").isEqualTo(0)
        .jsonPath("$.totalPages").isEqualTo(6);
  }

  @Test
  void forwardsQueryParameters() {
    when(this.getCatalog.execute(1, 10, "ali", "READY", "year", "desc"))
        .thenReturn(Mono.just(CatalogPage.empty()));

    this.client.get()
        .uri("/web/catalog?page=1&size=10&q=ali&status=READY&sort=year&dir=desc")
        .exchange()
        .expectStatus().isOk()
        .expectBody()
        .jsonPath("$.items").isEmpty();

    org.mockito.Mockito.verify(this.getCatalog)
        .execute(1, 10, "ali", "READY", "year", "desc");
  }
}
