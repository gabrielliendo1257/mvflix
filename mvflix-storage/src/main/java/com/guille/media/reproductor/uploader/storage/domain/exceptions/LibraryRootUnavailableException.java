package com.guille.media.reproductor.uploader.storage.domain.exceptions;

/**
 * El root de una biblioteca LOCAL no es accesible en el momento del escaneo
 * (dispositivo desmontado, carpeta borrada, permisos perdidos...).
 */
public class LibraryRootUnavailableException extends RuntimeException {

    public LibraryRootUnavailableException(String message) {
        super(message);
    }
}