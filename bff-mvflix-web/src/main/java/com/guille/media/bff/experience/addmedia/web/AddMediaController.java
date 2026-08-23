package com.guille.media.bff.experience.addmedia.web;

import com.guille.media.bff.app.dto.MovieEnrichmentPreviewDto;
import com.guille.media.bff.app.dto.MovieEnrichmentSearchDto;
import com.guille.media.bff.experience.addmedia.application.PreviewMovieCandidate;
import com.guille.media.bff.experience.addmedia.application.SearchMovieCandidates;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Experiencia Add Media: paso 1, buscar y previsualizar candidatos. El front
 * habla de "películas candidatas"; los endpoints técnicos de enrichment de
 * Movies quedan detrás de esta intención.
 */
@RestController
@RequestMapping(value = "/web/add-media", produces = MediaType.APPLICATION_JSON_VALUE)
public class AddMediaController {

  private final SearchMovieCandidates searchMovieCandidates;
  private final PreviewMovieCandidate previewMovieCandidate;

  public AddMediaController(
      SearchMovieCandidates searchMovieCandidates,
      PreviewMovieCandidate previewMovieCandidate) {
    this.searchMovieCandidates = searchMovieCandidates;
    this.previewMovieCandidate = previewMovieCandidate;
  }

  @GetMapping("/candidates")
  public Flux<MovieEnrichmentSearchDto> candidates(
      @RequestParam String query, @RequestParam(required = false) Integer year) {
    return this.searchMovieCandidates.search(query, year);
  }

  @GetMapping("/candidates/{providerId}")
  public Mono<MovieEnrichmentPreviewDto> candidate(@PathVariable Long providerId) {
    return this.previewMovieCandidate.preview(providerId);
  }
}
