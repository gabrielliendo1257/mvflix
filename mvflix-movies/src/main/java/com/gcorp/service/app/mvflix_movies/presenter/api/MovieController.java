package com.gcorp.service.app.mvflix_movies.presenter.api;

import com.gcorp.service.app.mvflix_movies.domain.model.Movie;
import com.gcorp.service.app.mvflix_movies.domain.service.CreateMovieCommand;
import com.gcorp.service.app.mvflix_movies.domain.service.MovieService;
import com.gcorp.service.app.mvflix_movies.presenter.api.dto.CompleteMovieRequest;
import com.gcorp.service.app.mvflix_movies.presenter.api.dto.CreateMovieRequest;
import com.gcorp.service.app.mvflix_movies.presenter.api.dto.MovieResponse;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
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
@RequestMapping(
    path = "/api/v1/movies",
    produces = MediaType.APPLICATION_JSON_VALUE)
public class MovieController {

    private final MovieService movieService;
    private final MovieApiMapper mapper;

    public MovieController(MovieService movieService, MovieApiMapper mapper) {
        this.movieService = movieService;
        this.mapper = mapper;
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public Mono<MovieResponse> create(@RequestBody CreateMovieRequest request) {
        return this.movieService
                .create(new CreateMovieCommand(this.mapper.toMetadata(request)))
                .map(this.mapper::toResponse);
    }

    @GetMapping("/{id}")
    public Mono<MovieResponse> findById(@PathVariable Long id) {
        return this.movieService.findById(id).map(this.mapper::toResponse);
    }

    @GetMapping
    public Flux<MovieResponse> list(@RequestParam(defaultValue = "20") int limit) {
        return this.movieService.list(limit).map(this.mapper::toResponse);
    }

    @PostMapping(value = "/{id}/complete", consumes = MediaType.APPLICATION_JSON_VALUE)
    public Mono<MovieResponse> complete(
            @PathVariable Long id, @RequestBody CompleteMovieRequest request) {
        return this.movieService.complete(id, request.objectKey()).map(this.mapper::toResponse);
    }

    @DeleteMapping("/{id}")
    public Mono<ResponseEntity<Void>> delete(@PathVariable Long id) {
        return this.movieService.delete(id).thenReturn(ResponseEntity.noContent().build());
    }
}
