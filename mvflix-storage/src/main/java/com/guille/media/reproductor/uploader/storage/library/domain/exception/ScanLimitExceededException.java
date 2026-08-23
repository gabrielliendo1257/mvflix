package com.guille.media.reproductor.uploader.storage.library.domain.exception;

/**
 * El escaneo supero el limite de archivos descubiertos configurado
 * ({@code storage.scan-max-files}): el arbol es demasiado grande para
 * una biblioteca y se aborta para no saturar CPU/IO.
 */
public class ScanLimitExceededException extends RuntimeException {

    public ScanLimitExceededException(String message) {
        super(message);
    }
}