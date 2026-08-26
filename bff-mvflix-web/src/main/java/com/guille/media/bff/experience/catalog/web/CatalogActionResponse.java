package com.guille.media.bff.experience.catalog.web;

import com.guille.media.bff.experience.catalog.application.port.CatalogActions;

/** Forma HTTP del job encolado por una acción de catálogo. */
public record CatalogActionResponse(
    String jobId,
    String status,
    int total,
    int done,
    int failed) {

  public static CatalogActionResponse from(CatalogActions.CatalogActionJob job) {
    return new CatalogActionResponse(
        job.jobId(), job.status(), job.total(), job.done(), job.failed());
  }
}
