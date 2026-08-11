package com.guille.media.bff.presenter.api;

import com.guille.media.bff.app.dto.CreateMovieRequest;
import com.guille.media.bff.app.dto.MovieDto;
import com.guille.media.bff.app.service.WebMoviesService;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/web/movies")
public class WebMoviesController {

  private final WebMoviesService webMoviesService;

  public WebMoviesController(WebMoviesService webMoviesService) {
    this.webMoviesService = webMoviesService;
  }

  @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
  public Flux<MovieDto> list(@RequestParam(defaultValue = "50") int limit) {
    return this.webMoviesService.list(limit);
  }

  @GetMapping(value = "/{movieId}", produces = MediaType.APPLICATION_JSON_VALUE)
  public Mono<ResponseEntity<MovieDto>> findById(@PathVariable Long movieId) {
    return this.webMoviesService.findById(movieId).map(ResponseEntity::ok);
  }

  @PostMapping(
      produces = MediaType.APPLICATION_JSON_VALUE,
      consumes = MediaType.APPLICATION_JSON_VALUE)
  public Mono<ResponseEntity<MovieDto>> create(@RequestBody CreateMovieRequest request) {
    return this.webMoviesService.create(request).map(ResponseEntity::ok);
  }
}
