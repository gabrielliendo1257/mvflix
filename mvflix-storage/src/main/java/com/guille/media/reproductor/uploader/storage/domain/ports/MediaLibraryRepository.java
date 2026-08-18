package com.guille.media.reproductor.uploader.storage.domain.ports;

import com.guille.media.reproductor.uploader.storage.domain.models.MediaLibrary;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface MediaLibraryRepository {

    Mono<MediaLibrary> findById(Long id);

    Mono<MediaLibrary> findByRootPath(String rootPath);

    Flux<MediaLibrary> findAllEnabled();

    Mono<MediaLibrary> save(MediaLibrary library);
}