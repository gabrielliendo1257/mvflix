package com.guille.media.reproductor.uploader.storage.domain.models;

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
  private final StorageSessionStatus storageObjectStatus;

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
