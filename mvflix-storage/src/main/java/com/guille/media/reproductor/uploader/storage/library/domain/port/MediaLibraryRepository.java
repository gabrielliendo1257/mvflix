package com.guille.media.reproductor.uploader.storage.library.domain.port;

import com.guille.media.reproductor.uploader.storage.library.domain.model.MediaLibrary;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface MediaLibraryRepository {

    Mono<MediaLibrary> findById(Long id);

    Mono<MediaLibrary> findByRootPath(String rootPath);

    /** Bibliotecas visibles para un usuario: las del operador más las propias. */
    Flux<MediaLibrary> findAllAccessibleTo(String ownerUsername);

    Mono<MediaLibrary> save(MediaLibrary library);

    Mono<Void> deleteById(Long id);
}
