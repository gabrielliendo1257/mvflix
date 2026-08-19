package com.guille.media.bff.app.ports;

import com.guille.media.bff.app.dto.CreateMovieRequest;
import com.guille.media.bff.app.dto.DiscoveredFileDto;
import com.guille.media.bff.app.dto.MediaAssetDto;
import com.guille.media.bff.app.dto.MovieDto;
import com.guille.media.bff.app.dto.MovieEnrichmentPreviewDto;
import com.guille.media.bff.app.dto.MovieEnrichmentSearchDto;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

/** Contrato hacia mvflix-movies (catálogo de películas del usuario). */
public interface MoviesWebClient {

  Flux<MovieDto> listMovies(int limit);

  Mono<MovieDto> movieById(Long movieId);

  Mono<MovieDto> createMovie(CreateMovieRequest request);

  /** Transición DRAFT -> READY con el object_id (visible al front) y object_key (solo servidores). */
  Mono<MovieDto> completeMovie(Long movieId, Long objectId, String objectKey);

  /** Rollback: elimina la película del dueño. */
  Mono<Void> deleteMovie(Long movieId);

  /** Candidatos de la fuente externa para el autocompletado interactivo. */
  Flux<MovieEnrichmentSearchDto> searchCandidates(String query, Integer year);

  /** Metadata de un candidato sin persistir, para que el usuario confirme. */
  Mono<MovieEnrichmentPreviewDto> previewCandidate(Long tmdbId);

  /** Autocompletado con el candidato elegido por el usuario. */
  Mono<MovieDto> enrichMovie(Long movieId, Long tmdbId);

  /** Upsert de los archivos que el storage descubrió en una biblioteca (media server). */
  Flux<MediaAssetDto> scanLibrary(Long storageId, List<DiscoveredFileDto> files);

  /** Activos de una biblioteca, opcionalmente filtrados por estado. */
  Flux<MediaAssetDto> listAssets(Long storageId, String status);

  /** Un activo por id (para derivar el título del filename al identificar). */
  Mono<MediaAssetDto> assetById(Long assetId);

  /** Activo de una película (playback LOCAL: storageId + relativePath). */
  Mono<MediaAssetDto> assetByMovie(Long movieId);

  /** Vincula el activo a una película nueva (media server). */
  Mono<MediaAssetDto> identifyAsset(Long assetId, String title, Long tmdbId);

  /** Cambia la visibilidad (PUBLIC/PRIVATE/SHARED); solo el dueño. */
  Mono<MovieDto> updateVisibility(Long movieId, String visibility);

  /** Reemplaza la lista de usuarios compartidos; solo el dueño. */
  Mono<MovieDto> updateShares(Long movieId, List<String> usernames);
}
