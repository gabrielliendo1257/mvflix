package com.guille.media.reproductor.uploader.storage.domain.ports;

import com.guille.media.reproductor.uploader.storage.domain.models.MediaLibrary;
import com.guille.media.reproductor.uploader.storage.domain.vos.LibraryFileHandle;

import reactor.core.publisher.Mono;

/**
 * Resolución de contenido de una biblioteca ya registrada: convierte un
 * relativePath en un handle validado (contenido dentro del root real,
 * anti path-traversal). Mono vacío si el archivo no existe o escapa del root.
 */
public interface LibraryContentResolver {

    Mono<LibraryFileHandle> resolve(MediaLibrary library, String relativePath);
}
