package com.gcorp.service.app.mvflix_movies.domain.mediaasset;

import com.gcorp.service.app.mvflix_movies.domain.movie.MovieId;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface MediaAssetRepository {

    Mono<MediaAsset> save(MediaAsset asset);

    Mono<MediaAsset> findById(MediaAssetId id);

    /** Compare-and-set: vincula únicamente si el asset continúa sin identificar. */
    Mono<MediaAsset> identifyIfUnidentified(MediaAssetId assetId, MovieId movieId);

    Mono<MediaAsset> findByMovieId(MovieId movieId);

    /** Desvincula los assets de una película sin borrar el catálogo del filesystem. */
    Mono<Long> unlinkByMovieId(MovieId movieId);

    Mono<MediaAsset> findByLibraryAndPath(Long libraryId, String relativePath);

    Flux<MediaAsset> findAllByLibraryId(Long libraryId);

    Flux<MediaAsset> findAllByLibraryIdAndStatus(Long libraryId, MediaAssetStatus status);
}
