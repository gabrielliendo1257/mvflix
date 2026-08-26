package com.guille.media.bff.experience.catalog.application.port;

import com.guille.media.bff.experience.catalog.application.CatalogPage;

import reactor.core.publisher.Mono;

/**
 * Proyección owned del catálogo tal como la necesita esta experiencia.
 * Aísla a application tanto del puerto global legacy como de las anotaciones
 * Jackson del downstream (esas viven en el adapter de infraestructura).
 */
public interface CatalogProjection {

  /** Parámetros ya normalizados por el caller (page>=0, size capado, q trim). */
  Mono<CatalogPage> page(
      int page, int size, String search, String status, String sort, String direction);
}
