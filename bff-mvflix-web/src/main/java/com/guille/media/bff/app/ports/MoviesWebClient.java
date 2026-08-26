package com.guille.media.bff.app.ports;

import com.guille.media.bff.app.dto.BulkVisibilityResultDto;
import com.guille.media.bff.app.dto.CreateMovieRequest;
import com.guille.media.bff.app.dto.DiscoveredFileDto;
import com.guille.media.bff.app.dto.MediaAssetDto;
import com.guille.media.bff.app.dto.MovieDto;
import com.guille.media.bff.app.dto.MovieEnrichmentPreviewDto;
import com.guille.media.bff.app.dto.MovieEnrichmentSearchDto;
import com.guille.media.bff.app.dto.MovieUpdateRequest;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

/** Contrato hacia mvflix-movies (catálogo de películas del usuario). */
public interface MoviesWebClient {

  Flux<MovieDto> listMovies(int limit);

  /** Contenido PROPIO del sujeto (lectura de administración, scope=owned). */
  Flux<MovieDto> listOwnedMovies(int limit);

  /**
   * Proyección owned paginada para la grilla de administración
   * (movies:/api/v1/catalog). Parámetros ya normalizados por el caller.
   */
  Mono<com.guille.media.bff.experience.catalog.application.CatalogPage> catalogPage(
      int page, int size, String search, String status, String sort, String direction);

  Mono<MovieDto> movieById(Long movieId);

  Mono<MovieDto> createMovie(CreateMovieRequest request);

  /** Alta guiada: draft identificado con acceso inicial aplicado por Movies. */
  Mono<MovieDto> createIdentifiedDraft(
      CreateMovieRequest draft, Long tmdbId, String visibility, java.util.List<String> sharedWith);

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

  /** Desvincula la película del proveedor externo (tmdbId/poster/popularity a null). */
  Mono<MovieDto> unlinkEnrichment(Long movieId);

  /** Upsert de los archivos que el storage descubrió en una biblioteca (media server). */
  Flux<MediaAssetDto> scanLibrary(Long libraryId, List<DiscoveredFileDto> files);

  /** Activos de una biblioteca, opcionalmente filtrados por estado. */
  Flux<MediaAssetDto> listAssets(Long libraryId, String status);

  /** Un activo por id (para derivar el título del filename al identificar). */
  Mono<MediaAssetDto> assetById(Long assetId);

  /** Activo de una película (playback LOCAL: libraryId + relativePath). */
  Mono<MediaAssetDto> assetByMovie(Long movieId);

  /** Vincula el activo a una película nueva (media server). */
  Mono<MediaAssetDto> identifyAsset(Long assetId, String title, Long tmdbId, String kind);

  /** Cambia la visibilidad (PUBLIC/PRIVATE/SHARED); solo el dueño. */
  Mono<MovieDto> updateVisibility(Long movieId, String visibility);

  /** Reemplaza la lista de usuarios compartidos; solo el dueño. */
  Mono<MovieDto> updateShares(Long movieId, List<String> usernames);

  /** Cambio de visibilidad en lote (movieIds y/o libraryIds); solo el dueño.
   *  El token se pasa explícito porque el trabajo corre fuera del contexto de
   *  la request (subscribe asíncrono) y el filtro de salida no lo vería. */
  Mono<BulkVisibilityResultDto> bulkUpdateVisibility(
      List<Long> movieIds, List<Long> libraryIds, String visibility, List<String> usernames,
      String accessToken);

  /** Edición manual de la metadata (merge: null conserva el valor actual); solo el dueño. */
  Mono<MovieDto> updateMovie(Long movieId, MovieUpdateRequest request);
}
