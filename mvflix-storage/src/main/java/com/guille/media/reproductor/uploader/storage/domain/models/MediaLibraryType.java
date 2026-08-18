package com.guille.media.reproductor.uploader.storage.domain.models;

public enum MediaLibraryType {
    /** Biblioteca local: root es un directorio del filesystem del operador. */
    LOCAL,

    /** Biblioteca gestionada: root es un bucket/prefix S3-compatible (reservado). */
    MANAGED
}