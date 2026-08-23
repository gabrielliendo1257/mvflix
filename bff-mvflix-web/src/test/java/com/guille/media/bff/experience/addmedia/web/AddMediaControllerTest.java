package com.guille.media.bff.experience.addmedia.web;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.guille.media.bff.app.dto.MovieEnrichmentPreviewDto;
import com.guille.media.bff.app.dto.MovieEnrichmentSearchDto;
import com.guille.media.bff.app.ports.MoviesWebClient;
import com.guille.media.bff.experience.addmedia.application.PreviewMovieCandidate;
import com.guille.media.bff.experience.addmedia.application.SearchMovieCandidates;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.reactive.server.WebTestClient;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

class AddMediaControllerTest {

  private final MoviesWebClient moviesWebClient = mock(MoviesWebClient.class);

  private WebTestClient client;

  @BeforeEach
  void setUp() {
    AddMediaController controller =
        new AddMediaController(
            new SearchMovieCandidates(this.moviesWebClient),
            new PreviewMovieCandidate(this.moviesWebClient));
    this.client = WebTestClient.bindToController(controller).build();
  }

  @Test
  void candidatesForwardsQueryAndYear() {
    when(this.moviesWebClient.searchCandidates("Alien", 1979))
        .thenReturn(Flux.just(new MovieEnrichmentSearchDto(348L, "Alien", 1979, "poster.png", "overview", "/enrich")));

    this.client
        .get()
        .uri("/web/add-media/candidates?query=Alien&year=1979")
        .exchange()
        .expectStatus()
        .isOk()
        .expectBody()
        .jsonPath("$[0].tmdb_id")
        .isEqualTo(348);
  }

  @Test
  void candidatePreviewForwardsProviderId() {
    when(this.moviesWebClient.previewCandidate(348L))
        .thenReturn(Mono.just(new MovieEnrichmentPreviewDto("Alien", "Alien", 1979,
            java.util.List.of("Horror"), 8.0, "1h 57m", "Ridley Scott",
            java.util.List.of("Sigourney Weaver"), "Overview...", "poster.png",
            "1979-05-25", "UK", "English", 348L)));

    this.client
        .get()
        .uri("/web/add-media/candidates/348")
        .exchange()
        .expectStatus()
        .isOk()
        .expectBody()
        .jsonPath("$.tmdb_id")
        .isEqualTo(348);
  }
}
