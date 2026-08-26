package com.guille.media.bff.experience.catalog.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.guille.media.bff.app.dto.BulkVisibilityRequest;
import com.guille.media.bff.app.service.Job;
import com.guille.media.bff.app.service.JobStatus;
import com.guille.media.bff.app.service.WebMoviesService;
import com.guille.media.bff.experience.catalog.application.CatalogPage;
import com.guille.media.bff.experience.catalog.application.GetCatalog;
import com.guille.media.bff.experience.catalog.web.ChangeVisibilityAction;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.reactive.server.WebTestClient;

import reactor.core.publisher.Mono;

import java.util.List;

class CatalogControllerTest {

  private final GetCatalog getCatalog = mock(GetCatalog.class);
  private final WebMoviesService webMoviesService = mock(WebMoviesService.class);
  private WebTestClient client;

  @BeforeEach
  void setUp() {
    this.client =
        WebTestClient.bindToController(new CatalogController(this.getCatalog, this.webMoviesService))
            .build();
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

  @Test
  void changeVisibilityDelegatesAsTypedBulkActionAndAccepts() {
    Job running = new Job("job-1", "pepe",
        com.guille.media.bff.app.service.JobType.BULK_VISIBILITY,
        JobStatus.RUNNING, 3, 1, 0, java.time.Instant.now(), java.time.Instant.now());
    when(this.webMoviesService.bulkVisibility(org.mockito.ArgumentMatchers.any()))
        .thenReturn(Mono.just(running));

    this.client.post()
        .uri("/web/catalog/actions/change-visibility")
        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
        .bodyValue(new ChangeVisibilityAction(
            List.of(7L), List.of(), "PUBLIC", List.of()))
        .exchange()
        .expectStatus().isEqualTo(org.springframework.http.HttpStatus.ACCEPTED)
        .expectBody()
        .jsonPath("$.id").isEqualTo("job-1")
        .jsonPath("$.status").isEqualTo("RUNNING");

    org.mockito.Mockito.verify(this.webMoviesService).bulkVisibility(
        new BulkVisibilityRequest(List.of(7L), List.of(), "PUBLIC", List.of()));
  }

  @Test
  void changeVisibilityWithoutVisibilityIsRejectedByThePolicyOwner() {
    when(this.webMoviesService.bulkVisibility(org.mockito.ArgumentMatchers.any()))
        .thenReturn(Mono.error(new org.springframework.web.server.ResponseStatusException(
            org.springframework.http.HttpStatus.BAD_REQUEST, "VISIBILITY_REQUIRED")));

    this.client.post()
        .uri("/web/catalog/actions/change-visibility")
        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
        .bodyValue(new ChangeVisibilityAction(List.of(7L), List.of(), null, List.of()))
        .exchange()
        .expectStatus().isBadRequest();

    assertThat(true).isTrue();
  }
}
