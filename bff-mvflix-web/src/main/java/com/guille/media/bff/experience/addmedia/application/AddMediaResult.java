package com.guille.media.bff.experience.addmedia.application;

import com.guille.media.bff.app.dto.UploadSessionDto;
import com.guille.media.bff.experience.addmedia.model.AddMediaProcess;
import com.guille.media.bff.experience.addmedia.model.AddMediaPhase;

/**
 * Vista del proceso Add Media orientada a pantalla. Refleja la fase de
 * EXPERIENCIA y, cuando existen, las instrucciones de subida directa.
 */
public record AddMediaResult(
    String addMediaId,
    String ownerSubject,
    AddMediaPhase phase,
    Long movieId,
    Long uploadId,
    UploadInstructions upload,
    String failureCode) {

  /** Instrucciones para que el navegador suba directo al object store. */
  public record UploadInstructions(
      String url,
      String method,
      String storageKey,
      long expectedSizeBytes,
      String expectedMimeType) {}

  public static AddMediaResult from(AddMediaProcess process) {
    return new AddMediaResult(
        process.id().value(),
        process.ownerSubject(),
        process.phase(),
        process.movieId(),
        process.uploadId(),
        null,
        process.failureCode());
  }

  public static AddMediaResult waitingForUpload(AddMediaProcess process, UploadSessionDto session) {
    return new AddMediaResult(
        process.id().value(),
        process.ownerSubject(),
        process.phase(),
        process.movieId(),
        process.uploadId(),
        new UploadInstructions(
            session.uploadUrl(),
            session.method(),
            session.storageKey(),
            session.object() == null ? 0L : session.object().expectedSize(),
            session.object() == null ? null : session.object().expectedMime()),
        null);
  }
}
