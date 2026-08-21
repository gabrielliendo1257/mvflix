package com.guille.media.bff.infrastructure.http;

import com.guille.media.bff.app.dto.BulkVisibilityRequest;
import com.guille.media.bff.app.dto.BulkVisibilityResultDto;
import com.guille.media.bff.app.dto.CreateMovieRequest;
import com.guille.media.bff.app.dto.DiscoveredFileDto;
import com.guille.media.bff.app.dto.IdentifyAssetRequest;
import com.guille.media.bff.app.dto.MediaAssetDto;
import com.guille.media.bff.app.dto.MovieDto;
import com.guille.media.bff.app.dto.MovieEnrichmentPreviewDto;
import com.guille.media.bff.app.dto.MovieEnrichmentRequest;
import com.guille.media.bff.app.dto.MovieEnrichmentSearchDto;
import com.guille.media.bff.app.dto.MovieSharesRequest;
import com.guille.media.bff.app.dto.MovieUpdateRequest;
import com.guille.media.bff.app.dto.MovieVisibilityRequest;
import com.guille.media.bff.app.dto.MoviesCompletePayload;
import com.guille.media.bff.app.ports.MoviesWebClient;

import lombok.RequiredArgsConstructor;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class MoviesWebClientAdapter implements MoviesWebClient {

  private static final String API = "/api/v1/movies";

  private final WebClient moviesWebClient;

  @Override
  public Flux<MovieDto> listMovies(int limit) {
    return this.moviesWebClient
        .get()
        .uri(uriBuilder -> uriBuilder.path(API).queryParam("limit", limit).build())
        .retrieve()
        .bodyToFlux(MovieDto.class);
  }

  @Override
  public Mono<MovieDto> movieById(Long movieId) {
    return this.moviesWebClient
        .get()
        .uri(API + "/" + movieId)
        .retrieve()
        .bodyToMono(MovieDto.class);
  }

  @Override
  public Mono<MovieDto> createMovie(CreateMovieRequest request) {
    return this.moviesWebClient
        .post()
        .uri(API)
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(request)
        .retrieve()
        .bodyToMono(MovieDto.class);
  }

  @Override
  public Mono<MovieDto> completeMovie(Long movieId, Long objectId, String objectKey) {
    return this.moviesWebClient
        .post()
        .uri(API + "/" + movieId + "/complete")
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(new MoviesCompletePayload(objectId, objectKey))
        .retrieve()
        .bodyToMono(MovieDto.class);
  }

  @Override
  public Mono<Void> deleteMovie(Long movieId) {
    return this.moviesWebClient
        .delete()
        .uri(API + "/" + movieId)
        .retrieve()
        .toBodilessEntity()
        .then();
  }

  @Override
  public Flux<MovieEnrichmentSearchDto> searchCandidates(String query, Integer year) {
    return this.moviesWebClient
        .get()
        .uri(uriBuilder -> uriBuilder
            .path(API + "/enrich/search")
            .queryParam("query", query)
            .queryParamIfPresent("year", java.util.Optional.ofNullable(year))
            .build())
        .retrieve()
        .bodyToFlux(MovieEnrichmentSearchDto.class);
  }

  @Override
  public Mono<MovieEnrichmentPreviewDto> previewCandidate(Long tmdbId) {
    return this.moviesWebClient
        .get()
        .uri(uriBuilder -> uriBuilder
            .path(API + "/enrich/preview")
            .queryParam("tmdb_id", tmdbId)
            .build())
        .retrieve()
        .bodyToMono(MovieEnrichmentPreviewDto.class);
  }

  @Override
  public Mono<MovieDto> enrichMovie(Long movieId, Long tmdbId) {
    return this.moviesWebClient
        .post()
        .uri(API + "/" + movieId + "/enrich")
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(new MovieEnrichmentRequest(tmdbId))
        .retrieve()
        .bodyToMono(MovieDto.class);
  }

  @Override
  public Mono<MovieDto> unlinkEnrichment(Long movieId) {
    return this.moviesWebClient
        .delete()
        .uri(API + "/" + movieId + "/enrich")
        .retrieve()
        .bodyToMono(MovieDto.class);
  }

  @Override
  public Flux<MediaAssetDto> scanLibrary(Long libraryId, List<DiscoveredFileDto> files) {
    return this.moviesWebClient
        .post()
        .uri(API + "/libraries/" + libraryId + "/scan")
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(Map.of("files", files))
        .retrieve()
        .bodyToFlux(MediaAssetDto.class);
  }

  @Override
  public Flux<MediaAssetDto> listAssets(Long libraryId, String status) {
    return this.moviesWebClient
        .get()
        .uri(uriBuilder -> uriBuilder
            .path(API + "/libraries/" + libraryId + "/assets")
            .queryParamIfPresent("status", java.util.Optional.ofNullable(status))
            .build())
        .retrieve()
        .bodyToFlux(MediaAssetDto.class);
  }

  @Override
  public Mono<MediaAssetDto> assetById(Long assetId) {
    return this.moviesWebClient
        .get()
        .uri(API + "/media-assets/" + assetId)
        .retrieve()
        .bodyToMono(MediaAssetDto.class);
  }

  @Override
  public Mono<MediaAssetDto> assetByMovie(Long movieId) {
    return this.moviesWebClient
        .get()
        .uri(API + "/media-assets/by-movie/" + movieId)
        .retrieve()
        .bodyToMono(MediaAssetDto.class);
  }

  @Override
  public Mono<MediaAssetDto> identifyAsset(Long assetId, String title, Long tmdbId, String kind) {
    return this.moviesWebClient
        .post()
        .uri(API + "/media-assets/" + assetId + "/identify")
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(new IdentifyAssetRequest(title, tmdbId, kind))
        .retrieve()
        .bodyToMono(MediaAssetDto.class);
  }

  @Override
  public Mono<MovieDto> updateVisibility(Long movieId, String visibility) {
    return this.moviesWebClient
        .post()
        .uri(API + "/" + movieId + "/visibility")
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(new MovieVisibilityRequest(visibility, null))
        .retrieve()
        .bodyToMono(MovieDto.class);
  }

  @Override
  public Mono<MovieDto> updateShares(Long movieId, List<String> usernames) {
    return this.moviesWebClient
        .post()
        .uri(API + "/" + movieId + "/shares")
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(new MovieSharesRequest(usernames))
        .retrieve()
        .bodyToMono(MovieDto.class);
  }

  @Override
  public Mono<BulkVisibilityResultDto> bulkUpdateVisibility(
      List<Long> movieIds, List<Long> libraryIds, String visibility, List<String> usernames,
      String accessToken) {
    return this.moviesWebClient
        .post()
        .uri(API + "/visibility/bulk")
        .contentType(MediaType.APPLICATION_JSON)
        .headers(headers -> {
          if (accessToken != null && !accessToken.isBlank()) {
            headers.setBearerAuth(accessToken);
          }
        })
        .bodyValue(new BulkVisibilityRequest(movieIds, libraryIds, visibility, usernames))
        .retrieve()
        .bodyToMono(BulkVisibilityResultDto.class);
  }

  @Override
  public Mono<MovieDto> updateMovie(Long movieId, MovieUpdateRequest request) {
    return this.moviesWebClient
        .put()
        .uri(API + "/" + movieId)
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(request)
        .retrieve()
        .bodyToMono(MovieDto.class);
  }
}
