package com.gcorp.service.app.mvflix_movies.domain.mediaasset;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface MediaAssetRepository {

    Mono<MediaAsset> save(MediaAsset asset);

    Mono<MediaAsset> findById(MediaAssetId id);

    Mono<MediaAsset> findByStorageAndPath(Long storageId, String relativePath);

    Flux<MediaAsset> findAllByStorageId(Long storageId);

    Flux<MediaAsset> findAllByStorageIdAndStatus(Long storageId, MediaAssetStatus status);
}
