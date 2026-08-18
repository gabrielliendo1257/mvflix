package com.gcorp.service.app.mvflix_movies.presenter.api;

import com.gcorp.service.app.mvflix_movies.application.movie.CompleteMovieUseCase;
import com.gcorp.service.app.mvflix_movies.application.movie.CreateMovieCommand;
import com.gcorp.service.app.mvflix_movies.application.movie.CreateMovieUseCase;
import com.gcorp.service.app.mvflix_movies.application.movie.DeleteMovieUseCase;
import com.gcorp.service.app.mvflix_movies.application.movie.GetMovieUseCase;
import com.gcorp.service.app.mvflix_movies.application.movie.ListMoviesUseCase;
import com.gcorp.service.app.mvflix_movies.application.enrichment.EnrichMovieUseCase;
import com.gcorp.service.app.mvflix_movies.domain.movie.MovieId;
import com.gcorp.service.app.mvflix_movies.presenter.api.dto.CompleteMovieRequest;
import com.gcorp.service.app.mvflix_movies.presenter.api.dto.CreateMovieRequest;
import com.gcorp.service.app.mvflix_movies.presenter.api.dto.EnrichMovieRequest;
import com.gcorp.service.app.mvflix_movies.presenter.api.dto.EnrichMovieSearchResponse;
import com.gcorp.service.app.mvflix_movies.presenter.api.dto.EnrichmentPreviewResponse;
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

import java.util.List;

@RestController
@RequestMapping(
    path = "/api/v1/movies",
    produces = MediaType.APPLICATION_JSON_VALUE)
public class MovieController {

    private final CreateMovieUseCase createMovieUseCase;
    private final GetMovieUseCase getMovieUseCase;
    private final ListMoviesUseCase listMoviesUseCase;
    private final CompleteMovieUseCase completeMovieUseCase;
    private final DeleteMovieUseCase deleteMovieUseCase;
    private final EnrichMovieUseCase enrichMovieUseCase;
    private final MovieApiMapper mapper;

    public MovieController(
            CreateMovieUseCase createMovieUseCase,
            GetMovieUseCase getMovieUseCase,
            ListMoviesUseCase listMoviesUseCase,
            CompleteMovieUseCase completeMovieUseCase,
            DeleteMovieUseCase deleteMovieUseCase,
            EnrichMovieUseCase enrichMovieUseCase,
            MovieApiMapper mapper) {
        this.createMovieUseCase = createMovieUseCase;
        this.getMovieUseCase = getMovieUseCase;
        this.listMoviesUseCase = listMoviesUseCase;
        this.completeMovieUseCase = completeMovieUseCase;
        this.deleteMovieUseCase = deleteMovieUseCase;
        this.enrichMovieUseCase = enrichMovieUseCase;
        this.mapper = mapper;
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public Mono<MovieResponse> create(@RequestBody CreateMovieRequest request) {
        return this.createMovieUseCase
                .execute(new CreateMovieCommand(this.mapper.toMetadata(request)))
                .map(this.mapper::toResponse);
    }

    @GetMapping("/{id}")
    public Mono<MovieResponse> findById(@PathVariable Long id) {
        return this.getMovieUseCase.execute(MovieId.of(id)).map(this.mapper::toResponse);
    }

    @GetMapping
    public Flux<MovieResponse> list(@RequestParam(defaultValue = "20") int limit) {
        return this.listMoviesUseCase.execute(limit).map(this.mapper::toResponse);
    }

    @PostMapping(value = "/{id}/complete", consumes = MediaType.APPLICATION_JSON_VALUE)
    public Mono<MovieResponse> complete(
            @PathVariable Long id, @RequestBody CompleteMovieRequest request) {
        return this.completeMovieUseCase
                .execute(MovieId.of(id), request.objectId(), request.objectKey())
                .map(this.mapper::toResponse);
    }

    @DeleteMapping("/{id}")
    public Mono<ResponseEntity<Void>> delete(@PathVariable Long id) {
        return this.deleteMovieUseCase.execute(MovieId.of(id)).thenReturn(ResponseEntity.noContent().build());
    }

    @PostMapping(value = "/{id}/enrich", consumes = MediaType.APPLICATION_JSON_VALUE)
    public Mono<MovieResponse> enrich(
            @PathVariable Long id,
            @RequestBody(required = false) EnrichMovieRequest request) {
        return this.enrichMovieUseCase
                .enrichCurrentUser(
                        MovieId.of(id), request == null ? null : request.tmdbId())
                .map(this.mapper::toResponse);
    }

    @GetMapping("/enrich/search")
    public Mono<List<EnrichMovieSearchResponse>> search(
            @RequestParam String query, @RequestParam(required = false) Integer year) {
        return this.enrichMovieUseCase
                .search(query, year)
                .map(results -> results.stream().map(this.mapper::toSearchResponse).toList());
    }

    @GetMapping("/enrich/preview")
    public Mono<EnrichmentPreviewResponse> preview(@RequestParam Long tmdbId) {
        return this.enrichMovieUseCase.preview(tmdbId).map(this.mapper::toPreviewResponse);
    }
}
