package com.guille.media.bff.infrastructure.http;

import com.guille.media.bff.app.dto.CreateMovieRequest;
import com.guille.media.bff.app.dto.EnrichMovieRequest;
import com.guille.media.bff.app.dto.EnrichMovieSearchDto;
import com.guille.media.bff.app.dto.MovieDto;
import com.guille.media.bff.app.dto.MoviesCompletePayload;
import com.guille.media.bff.app.ports.MoviesWebClient;

import lombok.RequiredArgsConstructor;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

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
  public Flux<EnrichMovieSearchDto> searchCandidates(String query, Integer year) {
    return this.moviesWebClient
        .get()
        .uri(uriBuilder -> uriBuilder
            .path(API + "/enrich/search")
            .queryParam("query", query)
            .queryParamIfPresent("year", java.util.Optional.ofNullable(year))
            .build())
        .retrieve()
        .bodyToFlux(EnrichMovieSearchDto.class);
  }

  @Override
  public Mono<MovieDto> enrichMovie(Long movieId, Long tmdbId) {
    return this.moviesWebClient
        .post()
        .uri(API + "/" + movieId + "/enrich")
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(new EnrichMovieRequest(tmdbId))
        .retrieve()
        .bodyToMono(MovieDto.class);
  }
}
