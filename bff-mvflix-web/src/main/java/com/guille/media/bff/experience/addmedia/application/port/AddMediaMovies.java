package com.guille.media.bff.experience.addmedia.application.port;

import com.guille.media.bff.app.dto.CreateMovieRequest;
import com.guille.media.bff.app.dto.MovieDto;
import com.guille.media.bff.app.dto.MovieEnrichmentPreviewDto;
import com.guille.media.bff.app.dto.MovieEnrichmentSearchDto;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Puerto del contexto Add Media hacia MOVIES (dueño del catálogo y de TMDB).
 * Solo lo que el alta necesita: buscar/previsualizar candidatos, crear y
 * descartar drafts, y persistir el paso DRAFT → READY.
 */
public interface AddMediaMovies {

  Flux<MovieEnrichmentSearchDto> searchCandidates(String query, Integer year);

  Mono<MovieEnrichmentPreviewDto> previewCandidate(Long providerId);

  /** Crea la película en estado DRAFT. La política de usuario bloqueado se
   * aplica aguas arriba (users); Movies valida visibilidad y catálogo. */
  Mono<MovieDto> createDraft(CreateMovieRequest draft);

  Mono<MovieDto> getMovie(Long movieId);

  /** Persiste la asociación del objeto subido y marca READY. */
  Mono<MovieDto> completeDraft(Long movieId, Long objectId, String objectKey);

  /** Compensación: elimina el draft creado por este proceso. */
  Mono<Void> discardDraft(Long movieId);
}
