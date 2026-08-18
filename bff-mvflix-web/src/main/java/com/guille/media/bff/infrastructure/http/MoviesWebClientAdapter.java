package com.guille.media.bff.infrastructure.http;

import com.guille.media.bff.app.dto.CreateMovieRequest;
import com.guille.media.bff.app.dto.DiscoveredFileDto;
import com.guille.media.bff.app.dto.MediaAssetDto;
import com.guille.media.bff.app.dto.MovieDto;
import com.guille.media.bff.app.dto.MovieEnrichmentPreviewDto;
import com.guille.media.bff.app.dto.MovieEnrichmentRequest;
import com.guille.media.bff.app.dto.MovieEnrichmentSearchDto;
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
  public Flux<MediaAssetDto> scanLibrary(Long storageId, List<DiscoveredFileDto> files) {
    return this.moviesWebClient
        .post()
        .uri(API + "/libraries/" + storageId + "/scan")
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(Map.of("files", files))
        .retrieve()
        .bodyToFlux(MediaAssetDto.class);
  }

  @Override
  public Flux<MediaAssetDto> listAssets(Long storageId, String status) {
    return this.moviesWebClient
        .get()
        .uri(uriBuilder -> uriBuilder
            .path(API + "/libraries/" + storageId + "/assets")
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
  public Mono<MediaAssetDto> identifyAsset(Long assetId, String title) {
    return this.moviesWebClient
        .post()
        .uri(API + "/media-assets/" + assetId + "/identify")
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(Map.of("title", title))
        .retrieve()
        .bodyToMono(MediaAssetDto.class);
  }
}
