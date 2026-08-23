package com.guille.media.reproductor.uploader.storage.library.domain.model;

public enum MediaLibraryType {
    /** Biblioteca local: root es un directorio del filesystem del operador. */
    LOCAL,

    /** Biblioteca gestionada: root es un bucket/prefix S3-compatible (reservado). */
    MANAGED
}