package com.guille.media.bff.experience.addmedia.application;

import com.guille.media.bff.app.dto.CreateMovieRequest;

import java.util.List;

/**
 * Intención completa del alta, ya sin envoltorio HTTP. El controller la
 * construye desde el request validado; el fingerprint de idempotencia se
 * calcula sobre ESTE objeto.
 */
public record StartAddMediaCommand(
    FileSelection file,
    MovieSelection movie,
    InitialAccess access,
    String idempotencyKey) {

  public record FileSelection(String filename, long sizeBytes, String mimeType) {}

  public record MovieSelection(Long providerId, CreateMovieRequest draft) {}

  public record InitialAccess(String visibility, List<String> sharedWith) {}
}
