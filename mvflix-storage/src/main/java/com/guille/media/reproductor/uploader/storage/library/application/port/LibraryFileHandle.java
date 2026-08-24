package com.guille.media.reproductor.uploader.storage.library.application.port;

/**
 * Handle de un archivo de biblioteca LOCAL resuelto y validado (contenido
 * dentro del root). Contrato de la capa de aplicación con el filesystem: el
 * {@code Path} absoluto es un detalle que solo consumen los adapters de
 * entrega (web/filesystem), nunca el dominio.
 */
public record LibraryFileHandle(
    String relativePath, java.nio.file.Path absolutePath, long size, String mimeType) {}