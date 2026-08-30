package com.gcorp.service.app.mvflix_movies.catalog.application;

import com.gcorp.service.app.mvflix_movies.catalog.domain.item.CatalogItemId;
import reactor.core.publisher.Mono;

public interface ManagedObjectIdLookup {
    Mono<Long> findObjectId(CatalogItemId movieId);
}
