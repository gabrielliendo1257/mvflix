package com.guille.media.bff.app.service;

import com.guille.media.bff.app.dto.CreateMovieRequest;
import com.guille.media.bff.app.dto.MovieDto;
import com.guille.media.bff.app.ports.MoviesWebClient;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class WebMoviesService {

  private final MoviesWebClient moviesWebClient;

  public Flux<MovieDto> list(int limit) {
    return this.moviesWebClient.listMovies(limit);
  }

  public Mono<MovieDto> findById(Long movieId) {
    return this.moviesWebClient.movieById(movieId);
  }

  public Mono<MovieDto> create(CreateMovieRequest request) {
    return this.moviesWebClient.createMovie(request);
  }
}
