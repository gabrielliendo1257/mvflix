package com.gcorp.service.app.mvflix_movies.catalog.application.port;

import com.gcorp.service.app.mvflix_movies.catalog.domain.item.CatalogItemId;
import reactor.core.publisher.Mono;

public interface IdentifiedDraftIdempotencyStore {
  record Claim(
      String actorId,
      String operation,
      String key,
      String requestHash,
      CatalogItemId movieId) {}

  Mono<Claim> claim(String actorId, String operation, String key, String requestHash);

  Mono<Void> bind(String actorId, String operation, String key, CatalogItemId movieId);
}
