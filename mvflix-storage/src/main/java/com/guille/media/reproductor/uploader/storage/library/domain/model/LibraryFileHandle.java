package com.guille.media.reproductor.uploader.storage.library.domain.model;

/**
 * Archivo de biblioteca LOCAL resuelto y validado (contenido en el root).
 * {@code absolutePath} es un detalle de infraestructura: no sale del storage.
 */
public record LibraryFileHandle(
    String relativePath, java.nio.file.Path absolutePath, long size, String mimeType) {}