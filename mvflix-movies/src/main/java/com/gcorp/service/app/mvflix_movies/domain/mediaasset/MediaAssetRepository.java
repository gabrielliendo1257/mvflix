package com.gcorp.service.app.mvflix_movies.domain.mediaasset;

import com.gcorp.service.app.mvflix_movies.domain.movie.MovieId;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface MediaAssetRepository {

    Mono<MediaAsset> save(MediaAsset asset);

    Mono<MediaAsset> findById(MediaAssetId id);

    Mono<MediaAsset> findByMovieId(MovieId movieId);

    Mono<MediaAsset> findByLibraryAndPath(Long libraryId, String relativePath);

    Flux<MediaAsset> findAllByLibraryId(Long libraryId);

    Flux<MediaAsset> findAllByLibraryIdAndStatus(Long libraryId, MediaAssetStatus status);
}
