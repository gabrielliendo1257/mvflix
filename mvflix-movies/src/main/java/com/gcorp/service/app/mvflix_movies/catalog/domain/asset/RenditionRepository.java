package com.gcorp.service.app.mvflix_movies.catalog.domain.asset;

import reactor.core.publisher.Mono;

public interface RenditionRepository {

    Mono<Rendition> save(Rendition rendition);

    Mono<Rendition> findById(RenditionId id);
}
