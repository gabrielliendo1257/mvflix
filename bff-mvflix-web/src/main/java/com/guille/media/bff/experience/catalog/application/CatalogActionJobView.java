package com.guille.media.bff.experience.catalog.application;

import com.guille.media.bff.experience.catalog.application.port.CatalogActions;

/** Vista HTTP del job encolado por una acción de catálogo. */
public record CatalogActionJobView(
    String jobId,
    String status,
    int total,
    int done,
    int failed) {

  public static CatalogActionJobView from(CatalogActions.CatalogActionJob job) {
    return new CatalogActionJobView(
        job.jobId(), job.status(), job.total(), job.done(), job.failed());
  }
}
