package com.guille.media.bff.infrastructure.http;

import com.guille.media.bff.app.dto.CreateMovieRequest;
import com.guille.media.bff.app.dto.MovieDto;
import com.guille.media.bff.app.dto.MovieEnrichmentPreviewDto;
import com.guille.media.bff.app.dto.MovieEnrichmentSearchDto;
import com.guille.media.bff.app.ports.MoviesWebClient;
import com.guille.media.bff.experience.addmedia.application.port.AddMediaMovies;

import com.guille.media.bff.experience.addmedia.application.port.AddMediaMovies.IdentifiedDraft;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Component;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/** Adapter HTTP del contexto Add Media hacia movies-service. Delega en el cliente existente. */
@Component
@RequiredArgsConstructor
public class MoviesAddMediaAdapter implements AddMediaMovies {

  private final MoviesWebClient delegate;

  @Override
  public Flux<MovieEnrichmentSearchDto> searchCandidates(String query, Integer year) {
    return this.delegate.searchCandidates(query, year);
  }

  @Override
  public Mono<MovieEnrichmentPreviewDto> previewCandidate(Long providerId) {
    return this.delegate.previewCandidate(providerId);
  }

  @Override
  public Mono<MovieDto> createIdentifiedDraft(IdentifiedDraft command) {
    return this.delegate.createIdentifiedDraft(
        command.draft(), command.tmdbId(), command.visibility(), command.sharedWith());
  }

  @Override
  public Mono<MovieDto> getMovie(Long movieId) {
    return this.delegate.movieById(movieId);
  }

  @Override
  public Mono<MovieDto> completeDraft(Long movieId, Long objectId, String objectKey) {
    return this.delegate.completeMovie(movieId, objectId, objectKey);
  }

  @Override
  public Mono<Void> discardDraft(Long movieId) {
    return this.delegate.deleteMovie(movieId);
  }
}
