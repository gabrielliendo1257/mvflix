package com.guille.media.bff.experience.catalog.application.port;

import reactor.core.publisher.Mono;

import java.util.List;

/** Acciones sobre la selección del catálogo; la orquestación es detalle de infraestructura. */
public interface CatalogActions {

  /** 202-aceptado: el progreso llega por el canal SSE de activity. */
  Mono<CatalogActionJob> changeVisibility(ActionRequest request);

  record ActionRequest(
      List<Long> movieIds,
      List<Long> libraryIds,
      String visibility,
      List<String> sharedWith) {}

  /** Vista mínima del job encolado, sin acoplar application al modelo legacy. */
  record CatalogActionJob(String jobId, String status, int total, int done, int failed) {}
}
