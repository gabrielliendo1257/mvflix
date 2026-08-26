package com.gcorp.service.app.mvflix_movies.catalog.application;

import reactor.core.publisher.Mono;

/** Puerto del read model: SQL de proyección, sin pasar por agregados. */
public interface CatalogViewRepository {

  Mono<CatalogPageView> page(CatalogReadQuery query);
}
