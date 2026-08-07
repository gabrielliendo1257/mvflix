package com.guille.media.reproductor.uploader.storage.domain.models;

import com.guille.media.reproductor.uploader.storage.domain.vos.StorageKey;
import com.guille.media.reproductor.uploader.storage.domain.vos.StorageLocation;
import com.guille.media.reproductor.uploader.storage.domain.vos.StorageMetadata;

import lombok.Getter;

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
 *   <li>La clave lógica del objeto ({@link StorageKey}).
 *   <li>La ubicación física ({@link StorageLocation}).
 *   <li>Los metadatos descriptivos ({@link StorageMetadata}).
 * </ul>
 *
 * <p>El dominio trabaja con esta entidad sin depender del proveedor concreto de almacenamiento.
 */
@Getter
public final class StoreObject {

    private final Long storageId;
    private final StorageKey storageKey;
    private final StorageMetadata metadata;
    private final StorageObjectStatus storageObjectStatus;


    public StoreObject(
            StorageKey storageKey,
            StorageMetadata metadata,
            Long storageId,
            StorageSessionStatus storageSessionStatus) {
        this.storageKey = Objects.requireNonNull(storageKey);
        this.metadata = metadata;
        this.storageId = storageId;
        this.storageObjectStatus = storageSessionStatus;
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

    public enum StorageSessionStatus {
        PROCESSING,
        DELETED,
        COMPLETED,
        PENDING
    }
}
