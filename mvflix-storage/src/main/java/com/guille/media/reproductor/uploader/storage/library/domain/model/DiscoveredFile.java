package com.guille.media.reproductor.uploader.storage.library.domain.model;

/**
 * Archivo descubierto por el scanner dentro del root de una biblioteca.
 * {@code relativePath} siempre es relativo al root (separador {@code /}); el resto
 * del sistema jamas ve paths absolutos.
 */
public record DiscoveredFile(String relativePath, long size, String mimeType) {}