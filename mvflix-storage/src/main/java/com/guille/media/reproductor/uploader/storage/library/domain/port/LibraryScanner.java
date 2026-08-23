package com.guille.media.reproductor.uploader.storage.library.domain.port;

import com.guille.media.reproductor.uploader.storage.library.domain.model.DiscoveredFile;

import reactor.core.publisher.Flux;

/**
 * Camina un root de biblioteca y descubre archivos multimedia soportados.
 * Deliberadamente tonto: no identifica contenido, no consulta metadata,
 * no toca el catálogo. Solo responde "esto existe aqui".
 */
public interface LibraryScanner {

    Flux<DiscoveredFile> scan(String rootPath);
}