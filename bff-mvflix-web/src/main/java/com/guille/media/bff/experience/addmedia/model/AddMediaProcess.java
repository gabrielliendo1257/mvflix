package com.guille.media.bff.experience.addmedia.model;

import java.util.Objects;

/**
 * Estado de proceso del alta de contenido, PROPIEDAD DEL BFF. El BFF es dueño
 * exclusivamente de:
 *
 * <ul>
 *   <li>la correlación movieId ↔ uploadId;</li>
 *   <li>la fase visible para la UX;</li>
 *   <li>el código de fallo orientado a pantalla.</li>
 * </ul>
 *
 * <p>NO copia información autoritativa: el estado real del objeto vive en
 * storage, la cuota en storage, y la visibilidad editorial en movies.
 * Las transiciones son inmutables (devuelven una nueva instancia).
 */
public record AddMediaProcess(
    AddMediaId id,
    String ownerSubject,
    Long movieId,
    Long uploadId,
    AddMediaPhase phase,
    String failureCode,
    long version) {

  public AddMediaProcess {
    Objects.requireNonNull(id, "id");
    Objects.requireNonNull(ownerSubject, "ownerSubject");
    if (version < 0) {
      throw new IllegalArgumentException("version no puede ser negativa");
    }
  }

  public static AddMediaProcess starting(AddMediaId id, String ownerSubject) {
    return new AddMediaProcess(id, ownerSubject, null, null, AddMediaPhase.STARTING, null, 0L);
  }

  public AddMediaProcess uploadPrepared(Long movieId, Long uploadId) {
    if (this.phase != AddMediaPhase.STARTING) {
      throw new InvalidAddMediaTransition(this.phase, AddMediaPhase.WAITING_FOR_UPLOAD);
    }
    return new AddMediaProcess(
        this.id, this.ownerSubject, movieId, uploadId,
        AddMediaPhase.WAITING_FOR_UPLOAD, null, this.version + 1);
  }

  public AddMediaProcess verifying() {
    if (this.phase != AddMediaPhase.WAITING_FOR_UPLOAD) {
      throw new InvalidAddMediaTransition(this.phase, AddMediaPhase.VERIFYING_UPLOAD);
    }
    return new AddMediaProcess(
        this.id, this.ownerSubject, this.movieId, this.uploadId,
        AddMediaPhase.VERIFYING_UPLOAD, null, this.version + 1);
  }

  public AddMediaProcess ready() {
    if (this.phase != AddMediaPhase.VERIFYING_UPLOAD
        && this.phase != AddMediaPhase.FINALIZING) {
      throw new InvalidAddMediaTransition(this.phase, AddMediaPhase.READY);
    }
    return new AddMediaProcess(
        this.id, this.ownerSubject, this.movieId, this.uploadId,
        AddMediaPhase.READY, null, this.version + 1);
  }

  public AddMediaProcess failed(String failureCode) {
    if (this.phase == AddMediaPhase.READY || this.phase == AddMediaPhase.CANCELLED) {
      throw new InvalidAddMediaTransition(this.phase, AddMediaPhase.FAILED);
    }
    return new AddMediaProcess(
        this.id, this.ownerSubject, this.movieId, this.uploadId,
        AddMediaPhase.FAILED, failureCode, this.version + 1);
  }

  public AddMediaProcess cancelled() {
    if (this.phase == AddMediaPhase.READY || this.phase == AddMediaPhase.CANCELLED) {
      throw new InvalidAddMediaTransition(this.phase, AddMediaPhase.CANCELLED);
    }
    return new AddMediaProcess(
        this.id, this.ownerSubject, this.movieId, this.uploadId,
        AddMediaPhase.CANCELLED, null, this.version + 1);
  }

  public boolean ownedBy(String subject) {
    return this.ownerSubject.equals(subject);
  }
}
