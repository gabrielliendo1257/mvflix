package com.guille.media.bff.experience.catalog.infrastructure.http;

import com.guille.media.bff.app.dto.BulkVisibilityRequest;
import com.guille.media.bff.app.service.WebMoviesService;
import com.guille.media.bff.experience.catalog.application.port.CatalogActions;
import com.guille.media.bff.experience.catalog.application.port.CatalogActions.ActionRequest;
import com.guille.media.bff.experience.catalog.application.port.CatalogActions.CatalogActionJob;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Component;

import reactor.core.publisher.Mono;

/**
 * Adapter del puerto hacia el orquestador legacy de bulk (JobStore + SSE).
 * Aísla el modelo Job fuera de application: cuando la orquestación migre a
 * su propia pieza, este archivo es el único que cambia.
 */
@Component
@RequiredArgsConstructor
public class CatalogActionsAdapter implements CatalogActions {

  private final WebMoviesService webMoviesService;

  @Override
  public Mono<CatalogActionJob> changeVisibility(ActionRequest request) {
    return this.webMoviesService
        .bulkVisibility(new BulkVisibilityRequest(
            request.movieIds(), request.libraryIds(),
            request.visibility(), request.sharedWith()))
        .map(job -> new CatalogActionJob(
            job.id(), job.status().name(), job.total(), job.done(), job.failed()));
  }
}
