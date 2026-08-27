package com.guille.media.reproductor.uploader.storage.managedstorage.domain.exception;

/**
 * El guard M2M de borrado detectó que la asociación del llamante no coincide
 * con el objeto real (owner o objectKey distintos). Borrar sería eliminar el
 * objeto equivocado. HTTP 409.
 */
public class StorageObjectMismatchException extends RuntimeException {

    private final String field;

    public StorageObjectMismatchException(String field, Long storageId) {
        super("Object " + field + " mismatch for storageId=" + storageId);
        this.field = field;
    }

    public String getField() {
        return this.field;
    }
}
