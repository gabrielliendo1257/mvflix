package com.gcorp.service.app.mvflix_movies.catalog.domain.asset;

import reactor.core.publisher.Mono;

public interface RenditionRepository {

    /**
     * Catalog only projects rendition states. Processing and the job that drives it
     * are responsibilities of an external pipeline.
     */
    Mono<Rendition> save(Rendition rendition);

    Mono<Rendition> findById(RenditionId id);

    Mono<Rendition> findBySourceAndProfile(MediaAssetId source, RenditionOrigin origin, String profile);
}
