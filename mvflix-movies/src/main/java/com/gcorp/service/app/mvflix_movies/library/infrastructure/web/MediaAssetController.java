package com.gcorp.service.app.mvflix_movies.library.infrastructure.web;

import com.gcorp.service.app.mvflix_movies.library.application.IdentifyAssetUseCase;
import com.gcorp.service.app.mvflix_movies.library.application.MediaAssetQueries;
import com.gcorp.service.app.mvflix_movies.library.application.ScanLibraryUseCase;
import com.gcorp.service.app.mvflix_movies.library.domain.MediaAssetId;
import com.gcorp.service.app.mvflix_movies.library.domain.MediaAssetStatus;
import com.gcorp.service.app.mvflix_movies.domain.movie.MovieId;
import com.gcorp.service.app.mvflix_movies.library.infrastructure.web.dto.IdentifyAssetRequest;
import com.gcorp.service.app.mvflix_movies.library.infrastructure.web.dto.MediaAssetResponse;
import com.gcorp.service.app.mvflix_movies.library.infrastructure.web.dto.ScanLibraryRequest;

import jakarta.validation.Valid;

import org.springframework.http.MediaType;
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
public class MediaAssetController {

    private final ScanLibraryUseCase scanLibraryUseCase;
    private final IdentifyAssetUseCase identifyAssetUseCase;
    private final MediaAssetQueries mediaAssetQueries;
    private final MediaAssetApiMapper mapper;

    public MediaAssetController(
            ScanLibraryUseCase scanLibraryUseCase,
            IdentifyAssetUseCase identifyAssetUseCase,
            MediaAssetQueries mediaAssetQueries,
            MediaAssetApiMapper mapper) {
        this.scanLibraryUseCase = scanLibraryUseCase;
        this.identifyAssetUseCase = identifyAssetUseCase;
        this.mediaAssetQueries = mediaAssetQueries;
        this.mapper = mapper;
    }

    /** El BFF entrega aqui los archivos que el storage descubrio en la biblioteca. */
    @PostMapping(value = "/libraries/{libraryId}/scan", consumes = MediaType.APPLICATION_JSON_VALUE)
    public Flux<MediaAssetResponse> scan(
            @PathVariable Long libraryId, @Valid @RequestBody ScanLibraryRequest request) {
        return this.scanLibraryUseCase
                .execute(libraryId, this.mapper.toScannedFiles(request))
                .map(this.mapper::toResponse);
    }

    @GetMapping("/libraries/{libraryId}/assets")
    public Flux<MediaAssetResponse> assets(
            @PathVariable Long libraryId,
            @RequestParam(required = false) String status) {
        MediaAssetStatus parsedStatus = status == null || status.isBlank()
                ? null
                : MediaAssetStatus.valueOf(status);
        return this.mediaAssetQueries
                .findByLibrary(libraryId, parsedStatus)
                .map(this.mapper::toResponse);
    }

    @GetMapping("/media-assets/{id}")
    public Mono<MediaAssetResponse> assetById(@PathVariable Long id) {
        return this.mediaAssetQueries
                .findById(MediaAssetId.of(id))
                .map(this.mapper::toResponse);
    }

    @GetMapping("/media-assets/by-movie/{movieId}")
    public Mono<MediaAssetResponse> assetByMovie(@PathVariable Long movieId) {
        return this.mediaAssetQueries
                .findByMovie(MovieId.of(movieId))
                .map(this.mapper::toResponse);
    }

    @PostMapping(value = "/media-assets/{id}/identify", consumes = MediaType.APPLICATION_JSON_VALUE)
    public Mono<MediaAssetResponse> identify(
            @PathVariable Long id, @Valid @RequestBody IdentifyAssetRequest request) {
        return this.identifyAssetUseCase
                .execute(MediaAssetId.of(id), request.title(), request.tmdbId(), request.kind())
                .map(this.mapper::toResponse);
    }
}
