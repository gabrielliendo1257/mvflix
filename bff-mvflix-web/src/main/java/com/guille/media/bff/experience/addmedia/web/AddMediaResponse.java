package com.guille.media.bff.experience.addmedia.web;

import com.guille.media.bff.experience.addmedia.application.AddMediaResult;
import com.guille.media.bff.experience.addmedia.model.AddMediaPhase;

/** Vista HTTP del resultado de aplicación (misma forma JSON que siempre). */
public record AddMediaResponse(
    String addMediaId,
    String ownerSubject,
    AddMediaPhase phase,
    Long movieId,
    Long uploadId,
    UploadInstructions upload,
    String failureCode) {

  public record UploadInstructions(
      String url,
      String method,
      String storageKey,
      long expectedSizeBytes,
      String expectedMimeType) {}

  public static AddMediaResponse from(AddMediaResult result) {
    return new AddMediaResponse(
        result.addMediaId(),
        result.ownerSubject(),
        result.phase(),
        result.movieId(),
        result.uploadId(),
        result.upload() == null ? null : new UploadInstructions(
            result.upload().url(),
            result.upload().method(),
            result.upload().storageKey(),
            result.upload().expectedSizeBytes(),
            result.upload().expectedMimeType()),
        result.failureCode());
  }
}
