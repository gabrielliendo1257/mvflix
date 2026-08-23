package com.guille.media.reproductor.uploader.storage.library.domain.port;

import com.guille.media.reproductor.uploader.storage.library.domain.model.MediaLibrary;
import com.guille.media.reproductor.uploader.storage.library.domain.model.LibraryFileHandle;

import reactor.core.publisher.Mono;

/**
 * Resolución de contenido de una biblioteca ya registrada: convierte un
 * relativePath en un handle validado (contenido dentro del root real,
 * anti path-traversal). Mono vacío si el archivo no existe o escapa del root.
 */
public interface LibraryContentResolver {

    Mono<LibraryFileHandle> resolve(MediaLibrary library, String relativePath);
}
