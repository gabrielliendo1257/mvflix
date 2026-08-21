package com.gcorp.service.app.mvflix_movies.presenter.api;

import com.gcorp.service.app.mvflix_movies.app.security.UserProvider;
import com.gcorp.service.app.mvflix_movies.application.scan.IdentifyAssetUseCase;
import com.gcorp.service.app.mvflix_movies.application.scan.ScanLibraryUseCase;
import com.gcorp.service.app.mvflix_movies.domain.mediaasset.MediaAsset;
import com.gcorp.service.app.mvflix_movies.domain.mediaasset.MediaAssetId;
import com.gcorp.service.app.mvflix_movies.domain.mediaasset.MediaAssetNotFoundException;
import com.gcorp.service.app.mvflix_movies.domain.mediaasset.MediaAssetRepository;
import com.gcorp.service.app.mvflix_movies.domain.mediaasset.MediaAssetStatus;
import com.gcorp.service.app.mvflix_movies.domain.movie.MovieAccessDeniedException;
import com.gcorp.service.app.mvflix_movies.domain.movie.MovieId;
import com.gcorp.service.app.mvflix_movies.domain.movie.MovieRepository;
import com.gcorp.service.app.mvflix_movies.presenter.api.dto.IdentifyAssetRequest;
import com.gcorp.service.app.mvflix_movies.presenter.api.dto.MediaAssetResponse;
import com.gcorp.service.app.mvflix_movies.presenter.api.dto.ScanLibraryRequest;

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
    private final MediaAssetRepository assetRepository;
    private final MovieRepository movieRepository;
    private final UserProvider userProvider;
    private final MediaAssetApiMapper mapper;

    public MediaAssetController(
            ScanLibraryUseCase scanLibraryUseCase,
            IdentifyAssetUseCase identifyAssetUseCase,
            MediaAssetRepository assetRepository,
            MovieRepository movieRepository,
            UserProvider userProvider,
            MediaAssetApiMapper mapper) {
        this.scanLibraryUseCase = scanLibraryUseCase;
        this.identifyAssetUseCase = identifyAssetUseCase;
        this.assetRepository = assetRepository;
        this.movieRepository = movieRepository;
        this.userProvider = userProvider;
        this.mapper = mapper;
    }

    /** El BFF entrega aqui los archivos que el storage descubrio en la biblioteca. */
    @PostMapping(value = "/libraries/{libraryId}/scan", consumes = MediaType.APPLICATION_JSON_VALUE)
    public Flux<MediaAssetResponse> scan(
            @PathVariable Long libraryId, @RequestBody ScanLibraryRequest request) {
        return this.scanLibraryUseCase
                .execute(libraryId, this.mapper.toScannedFiles(request))
                .map(this.mapper::toResponse);
    }

    @GetMapping("/libraries/{libraryId}/assets")
    public Flux<MediaAssetResponse> assets(
            @PathVariable Long libraryId,
            @RequestParam(required = false) String status) {
        Flux<MediaAsset> assets = status == null || status.isBlank()
                ? this.assetRepository.findAllByLibraryId(libraryId)
                : this.assetRepository.findAllByLibraryIdAndStatus(
                        libraryId, MediaAssetStatus.valueOf(status));
        return assets.map(this.mapper::toResponse);
    }

    @GetMapping("/media-assets/{id}")
    public Mono<MediaAssetResponse> assetById(@PathVariable Long id) {
        return this.assetRepository
                .findById(MediaAssetId.of(id))
                .switchIfEmpty(Mono.error(
                        new MediaAssetNotFoundException("Media asset not found: " + id)))
                .map(this.mapper::toResponse);
    }

    @GetMapping("/media-assets/by-movie/{movieId}")
    public Mono<MediaAssetResponse> assetByMovie(@PathVariable Long movieId) {
        return this.userProvider
                .getAuthenticatedUser()
                .flatMap(user -> this.movieRepository
                        .findById(MovieId.of(movieId))
                        .switchIfEmpty(Mono.defer(() -> Mono.error(
                                new MovieAccessDeniedException(
                                        "Movie not accessible: " + movieId))))
                        .filter(movie -> movie.isVisibleTo(user.subject()))
                        .switchIfEmpty(Mono.defer(() -> Mono.error(
                                new MovieAccessDeniedException(
                                        "Movie not accessible: " + movieId)))))
                .flatMap(movie -> this.assetRepository
                        .findByMovieId(MovieId.of(movieId))
                        .switchIfEmpty(Mono.defer(() -> Mono.error(
                                new MediaAssetNotFoundException(
                                        "No media asset for movie: " + movieId)))))
                .map(this.mapper::toResponse);
    }

    @PostMapping(value = "/media-assets/{id}/identify", consumes = MediaType.APPLICATION_JSON_VALUE)
    public Mono<MediaAssetResponse> identify(
            @PathVariable Long id, @RequestBody IdentifyAssetRequest request) {
        return this.identifyAssetUseCase
                .execute(MediaAssetId.of(id), request.title(), request.tmdbId(), request.kind())
                .map(this.mapper::toResponse);
    }
}
