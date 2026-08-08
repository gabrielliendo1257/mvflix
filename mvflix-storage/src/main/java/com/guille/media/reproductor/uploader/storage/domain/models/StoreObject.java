package com.guille.media.reproductor.uploader.storage.domain.models;

import com.guille.media.reproductor.uploader.storage.domain.exceptions.IllegalStateTransitionException;
import com.guille.media.reproductor.uploader.storage.domain.exceptions.InvalidObjectContentError;
import com.guille.media.reproductor.uploader.storage.domain.exceptions.StorageObjectNotAvailable;
import com.guille.media.reproductor.uploader.storage.domain.vos.StorageKey;
import com.guille.media.reproductor.uploader.storage.domain.vos.StorageMetadata;

import lombok.Getter;

import java.time.Instant;
import java.util.Objects;

/**
 * Representa un objeto almacenado lógicamente en el sistema.
 *
 * <p>Puede tratarse de:
 *
 * <ul>
 *   <li>Una película.
 *   <li>Un archivo de subtítulos.
 *   <li>Una imagen de portada.
 *   <li>Un documento adjunto.
 * </ul>
 *
 * <p>Esta entidad une:
 *
 * <ul>
 *   <li>El propietario del objeto.
 *   <li>La clave lógica del objeto ({@link StorageKey}).
 *   <li>Los metadatos descriptivos ({@link StorageMetadata}).
 * </ul>
 *
 * <p>El dominio trabaja con esta entidad sin depender del proveedor concreto de almacenamiento.
 */
@Getter
public final class StoreObject {

  private final Long storageId;
  private final String ownerUsername;
  private final StorageKey storageKey;
  private final StorageMetadata metadata;
  private final Instant createdAt;
  private StorageSessionStatus storageObjectStatus;

  public StoreObject(
      String ownerUsername,
      StorageKey storageKey,
      StorageMetadata metadata,
      Instant createdAt,
      Long storageId,
      StorageSessionStatus storageSessionStatus) {
    this.ownerUsername = Objects.requireNonNull(ownerUsername);
    this.storageKey = Objects.requireNonNull(storageKey);
    this.metadata = Objects.requireNonNull(metadata);
    this.createdAt = Objects.requireNonNull(createdAt);
    this.storageId = storageId;
    this.storageObjectStatus = Objects.requireNonNull(storageSessionStatus);
  }

  /**
   * @return tamaño del objeto en bytes.
   */
  public long sizeInBytes() {
    return metadata.contentLength();
  }

  /**
   * @return tipo MIME del objeto.
   */
  public String contentType() {
    return metadata.contentType();
  }

  /**
   * Determina si el objeto representa un video.
   *
   * @return true si el tipo MIME es de video.
   */
  public boolean isVideo() {
    return metadata.isVideo();
  }

  /**
   * Determina si el objeto representa una imagen.
   *
   * @return true si el tipo MIME es de imagen.
   */
  public boolean isImage() {
    return metadata.isImage();
  }

  /**
   * Determina si el objeto representa un archivo de subtítulos.
   *
   * @return true si el tipo MIME corresponde a subtítulos.
   */
  public boolean isSubtitle() {
    return metadata.isSubtitle();
  }

  public boolean isAvailable() {
    return this.storageObjectStatus == StorageSessionStatus.COMPLETED;
  }

  /**
   * Comprueba que el objeto está disponible para su consumo.
   *
   * @throws StorageObjectNotAvailable si el objeto aún no está completado.
   */
  public void ensureAvailable() {
    if (!isAvailable()) {
      throw new StorageObjectNotAvailable("Storage object not available: " + this.storageId);
    }
  }

  /**
   * Transición a {@code COMPLETED}: el upload terminó y el objeto es consumible.
   *
   * <p>Idempotente si el objeto ya está completado (devuelve {@code false}).
   *
   * @return {@code true} si se produjo la transición PENDING → COMPLETED.
   * @throws IllegalStateTransitionException si el objeto no está en {@code PENDING}.
   */
  public boolean complete() {
    if (this.storageObjectStatus == StorageSessionStatus.COMPLETED) {
      return false;
    }
    requireStatus(StorageSessionStatus.PENDING, "complete");
    this.storageObjectStatus = StorageSessionStatus.COMPLETED;
    return true;
  }

  /**
   * Transición a {@code EXPIRED}: la sesión de subida caducó.
   *
   * <p>Idempotente si el objeto ya está expirado (devuelve {@code false}).
   *
   * @return {@code true} si se realizó la transición PENDING → EXPIRED.
   * @throws IllegalStateTransitionException si el objeto no está en {@code PENDING}.
   */
  public boolean expire() {
    if (this.storageObjectStatus == StorageSessionStatus.EXPIRED) {
      return false;
    }
    requireStatus(StorageSessionStatus.PENDING, "expire");
    this.storageObjectStatus = StorageSessionStatus.EXPIRED;
    return true;
  }

  /**
   * Borrado lógico a {@code DELETED}.
   *
   * <p>Idempotente si el objeto ya está eliminado (devuelve {@code false}).
   *
   * @return {@code true} si se realizó la transición COMPLETED → DELETED.
   * @throws IllegalStateTransitionException si el objeto no está en {@code COMPLETED}.
   */
  public boolean markDeleted() {
    if (this.storageObjectStatus == StorageSessionStatus.DELETED) {
      return false;
    }
    requireStatus(StorageSessionStatus.COMPLETED, "delete");
    this.storageObjectStatus = StorageSessionStatus.DELETED;
    return true;
  }

  private void requireStatus(StorageSessionStatus expected, String transition) {
    if (this.storageObjectStatus != expected) {
      throw new IllegalStateTransitionException(
          "Cannot " + transition + " object " + this.storageId + ": current status is "
              + this.storageObjectStatus);
    }
  }

  /**
   * Comprueba que el objeto pertenece al usuario indicado.
   *
   * @param username propietario esperado.
   * @throws StorageObjectNotAvailable si el usuario no es el propietario (no se filtra la
   *     existencia del objeto por privacidad).
   */
  public void ensureOwnedBy(String username) {
    if (!Objects.equals(this.ownerUsername, username)) {
      throw new StorageObjectNotAvailable("Storage object not available: " + this.storageId);
    }
  }

  /**
   * Valida que el tamaño del objeto subido coincide con el tamaño esperado.
   *
   * @param contentLength tamaño real obtenido del proveedor de almacenamiento.
   * @throws InvalidObjectContentError si hay discrepancia de tamaño.
   */
  public void ensureValidContentLength(long contentLength) {
    if (contentLength != this.metadata.contentLength()) {
      throw new InvalidObjectContentError("Object size mismatch for upload: " + this.storageId);
    }
  }

  public enum StorageSessionStatus {
    PROCESSING,
    PENDING,
    COMPLETED,
    EXPIRED,
    DELETED
  }
}
