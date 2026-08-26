package com.gcorp.service.app.mvflix_movies.catalog.infrastructure.web;

import com.gcorp.service.app.mvflix_movies.catalog.application.BulkVisibilityUseCase;
import com.gcorp.service.app.mvflix_movies.catalog.application.CompleteMovieUseCase;
import com.gcorp.service.app.mvflix_movies.catalog.application.CreateMovieCommand;
import com.gcorp.service.app.mvflix_movies.catalog.application.CreateIdentifiedDraftCommand;
import com.gcorp.service.app.mvflix_movies.catalog.application.CreateIdentifiedDraftUseCase;
import com.gcorp.service.app.mvflix_movies.catalog.domain.movie.MovieMetadata;
import com.gcorp.service.app.mvflix_movies.catalog.application.CreateMovieUseCase;
import com.gcorp.service.app.mvflix_movies.catalog.application.DeleteMovieUseCase;
import com.gcorp.service.app.mvflix_movies.catalog.application.GetMovieUseCase;
import com.gcorp.service.app.mvflix_movies.catalog.application.ListMoviesUseCase;
import com.gcorp.service.app.mvflix_movies.catalog.application.UpdateMovieUseCase;
import com.gcorp.service.app.mvflix_movies.catalog.application.EnrichMovieUseCase;
import com.gcorp.service.app.mvflix_movies.catalog.domain.movie.MediaKind;
import com.gcorp.service.app.mvflix_movies.catalog.domain.movie.MovieVisibility;
import com.gcorp.service.app.mvflix_movies.catalog.domain.movie.MovieId;
import com.gcorp.service.app.mvflix_movies.catalog.infrastructure.web.dto.BulkVisibilityRequest;
import com.gcorp.service.app.mvflix_movies.catalog.infrastructure.web.dto.BulkVisibilityResponse;
import com.gcorp.service.app.mvflix_movies.catalog.infrastructure.web.dto.CompleteMovieRequest;
import com.gcorp.service.app.mvflix_movies.catalog.infrastructure.web.dto.CreateIdentifiedDraftRequest;
import com.gcorp.service.app.mvflix_movies.catalog.infrastructure.web.dto.CreateMovieRequest;
import com.gcorp.service.app.mvflix_movies.catalog.infrastructure.web.dto.EnrichMovieRequest;
import com.gcorp.service.app.mvflix_movies.catalog.infrastructure.web.dto.EnrichMovieSearchResponse;
import com.gcorp.service.app.mvflix_movies.catalog.infrastructure.web.dto.EnrichmentPreviewResponse;
import com.gcorp.service.app.mvflix_movies.catalog.infrastructure.web.dto.MovieResponse;
import com.gcorp.service.app.mvflix_movies.catalog.application.UpdateSharesUseCase;
import com.gcorp.service.app.mvflix_movies.catalog.application.UpdateVisibilityUseCase;
import com.gcorp.service.app.mvflix_movies.catalog.infrastructure.web.dto.UpdateMovieRequest;
import com.gcorp.service.app.mvflix_movies.catalog.infrastructure.web.dto.UpdateSharesRequest;
import com.gcorp.service.app.mvflix_movies.catalog.infrastructure.web.dto.UpdateVisibilityRequest;

import jakarta.validation.Valid;

import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

@Tag(name = "Movies", description = "Catálogo: drafts (incl. identificados), READY, visibilidad, shares, enriquecimiento TMDB")
@RestController
@RequestMapping(
    path = "/api/v1/movies",
    produces = MediaType.APPLICATION_JSON_VALUE)
public class MovieController {

    private final CreateMovieUseCase createMovieUseCase;
    private final CreateIdentifiedDraftUseCase createIdentifiedDraftUseCase;
    private final GetMovieUseCase getMovieUseCase;
    private final ListMoviesUseCase listMoviesUseCase;
    private final UpdateVisibilityUseCase updateVisibilityUseCase;
    private final UpdateSharesUseCase updateSharesUseCase;
    private final BulkVisibilityUseCase bulkVisibilityUseCase;
    private final UpdateMovieUseCase updateMovieUseCase;
    private final CompleteMovieUseCase completeMovieUseCase;
    private final DeleteMovieUseCase deleteMovieUseCase;
    private final EnrichMovieUseCase enrichMovieUseCase;
    private final MovieApiMapper mapper;

    public MovieController(
            CreateMovieUseCase createMovieUseCase,
            CreateIdentifiedDraftUseCase createIdentifiedDraftUseCase,
            GetMovieUseCase getMovieUseCase,
            ListMoviesUseCase listMoviesUseCase,
            UpdateVisibilityUseCase updateVisibilityUseCase,
            UpdateSharesUseCase updateSharesUseCase,
            BulkVisibilityUseCase bulkVisibilityUseCase,
            UpdateMovieUseCase updateMovieUseCase,
            CompleteMovieUseCase completeMovieUseCase,
            DeleteMovieUseCase deleteMovieUseCase,
            EnrichMovieUseCase enrichMovieUseCase,
            MovieApiMapper mapper) {
        this.createMovieUseCase = createMovieUseCase;
        this.createIdentifiedDraftUseCase = createIdentifiedDraftUseCase;
        this.getMovieUseCase = getMovieUseCase;
        this.listMoviesUseCase = listMoviesUseCase;
        this.updateVisibilityUseCase = updateVisibilityUseCase;
        this.updateSharesUseCase = updateSharesUseCase;
        this.bulkVisibilityUseCase = bulkVisibilityUseCase;
        this.updateMovieUseCase = updateMovieUseCase;
        this.completeMovieUseCase = completeMovieUseCase;
        this.deleteMovieUseCase = deleteMovieUseCase;
        this.enrichMovieUseCase = enrichMovieUseCase;
        this.mapper = mapper;
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public Mono<MovieResponse> create(@Valid @RequestBody CreateMovieRequest request) {
        MediaKind kind = request.kind() == null ? MediaKind.MOVIE : request.kind();
        return this.createMovieUseCase
                .execute(new CreateMovieCommand(this.mapper.toMetadata(request), kind))
                .map(this.mapper::toResponse);
    }

    /** Alta guiada (Add Media): draft identificado + acceso inicial, atómicos. */
    @PostMapping(
        value = "/identified-drafts",
        consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Draft identificado con acceso inicial, aplicado atómicamente")
  public Mono<MovieResponse> createIdentifiedDraft(
            @Valid @RequestBody CreateIdentifiedDraftRequest request) {
        return this.createIdentifiedDraftUseCase
                .execute(this.toCommand(request))
                .map(this.mapper::toResponse);
    }

    private CreateIdentifiedDraftCommand toCommand(CreateIdentifiedDraftRequest request) {
        MovieMetadata metadata = this.mapper.toMetadata(request.draft());
        if (request.tmdbId() != null && request.draft().kind() != MediaKind.OTHER) {
            metadata = metadata.withTmdbId(request.tmdbId());
        }
        return new CreateIdentifiedDraftCommand(
                metadata,
                request.draft().kind(),
                request.visibility() == null ? null : MovieVisibility.valueOf(request.visibility()),
                request.sharedWith() == null ? java.util.List.of() : java.util.List.copyOf(request.sharedWith()));
    }

    @GetMapping("/{id}")
    public Mono<MovieResponse> findById(@PathVariable Long id) {
        return this.getMovieUseCase.execute(MovieId.of(id)).map(this.mapper::toResponse);
    }

    @PostMapping("/{id}/visibility")
    public Mono<MovieResponse> updateVisibility(
            @PathVariable Long id, @Valid @RequestBody UpdateVisibilityRequest request) {
        return this.updateVisibilityUseCase
                .execute(MovieId.of(id), request.visibility())
                .map(this.mapper::toResponse);
    }

    @PostMapping("/{id}/shares")
    public Mono<MovieResponse> updateShares(
            @PathVariable Long id, @Valid @RequestBody UpdateSharesRequest request) {
        return this.updateSharesUseCase
                .execute(MovieId.of(id), request.usernames())
                .map(this.mapper::toResponse);
    }

    @PostMapping(value = "/visibility/bulk", consumes = MediaType.APPLICATION_JSON_VALUE)
    public Mono<BulkVisibilityResponse> bulkVisibility(
            @Valid @RequestBody BulkVisibilityRequest request) {
        List<MovieId> movieIds = request.movieIds() == null
                ? List.of()
                : request.movieIds().stream().map(MovieId::of).toList();
        List<Long> libraryIds = request.libraryIds() == null
                ? List.of()
                : request.libraryIds();
        return this.bulkVisibilityUseCase
                .execute(movieIds, libraryIds, request.visibility(), request.usernames())
                .map(result -> new BulkVisibilityResponse(
                        result.total(), result.updated(), result.failed()));
    }

    @GetMapping
    public Flux<MovieResponse> list(
            @RequestParam(defaultValue = "visible") String scope,
            @RequestParam(defaultValue = "20") int limit) {
        return this.listMoviesUseCase.execute(scope, limit).map(this.mapper::toResponse);
    }

    @PostMapping(value = "/{id}/complete", consumes = MediaType.APPLICATION_JSON_VALUE)
    public Mono<MovieResponse> complete(
            @PathVariable Long id, @Valid @RequestBody CompleteMovieRequest request) {
        return this.completeMovieUseCase
                .execute(MovieId.of(id), request.objectId(), request.objectKey())
                .map(this.mapper::toResponse);
    }

    /** Edición manual de la metadata del dueño (merge: null conserva el valor actual). */
    @PutMapping(value = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public Mono<MovieResponse> update(
            @PathVariable Long id, @Valid @RequestBody UpdateMovieRequest request) {
        return this.updateMovieUseCase
                .execute(MovieId.of(id), this.mapper.toCommand(request))
                .map(this.mapper::toResponse);
    }

    @DeleteMapping("/{id}")
    public Mono<ResponseEntity<Void>> delete(@PathVariable Long id) {
        return this.deleteMovieUseCase.execute(MovieId.of(id)).thenReturn(ResponseEntity.noContent().build());
    }

    @PostMapping(value = "/{id}/enrich", consumes = MediaType.APPLICATION_JSON_VALUE)
    public Mono<MovieResponse> enrich(
            @PathVariable Long id,
            @Valid @RequestBody(required = false) EnrichMovieRequest request) {
        return this.enrichMovieUseCase
                .enrichCurrentUser(
                        MovieId.of(id), request == null ? null : request.tmdbId())
                .map(this.mapper::toResponse);
    }

    @DeleteMapping("/{id}/enrich")
    public Mono<MovieResponse> unlinkEnrichment(@PathVariable Long id) {
        return this.enrichMovieUseCase
                .unlinkCurrentUser(MovieId.of(id))
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
    public Mono<EnrichmentPreviewResponse> preview(@RequestParam("tmdb_id") Long tmdbId) {
        return this.enrichMovieUseCase.preview(tmdbId).map(this.mapper::toPreviewResponse);
    }
}
