package com.guille.media.reproductor.uploader.storage.app.service;

import java.util.List;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;

import com.guille.media.reproductor.uploader.storage.app.security.UserProvider;
import com.guille.media.reproductor.uploader.storage.domain.models.MediaLibrary;
import com.guille.media.reproductor.uploader.storage.domain.ports.LibraryScanner;
import com.guille.media.reproductor.uploader.storage.domain.ports.MediaLibraryRepository;
import com.guille.media.reproductor.uploader.storage.domain.vos.DiscoveredFile;
import com.guille.media.reproductor.uploader.storage.infrastructure.errors.EntityNotFound;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Bibliotecas del media server: listado y escaneo. El scanner solo descubre
 * archivos; la identificacion y el catalogo viven en el servicio de movies.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LibraryService {

    private final MediaLibraryRepository libraryRepository;
    private final LibraryScanner libraryScanner;
    private final UserProvider userProvider;

    public Flux<MediaLibrary> listLibraries() {
        return this.userProvider
                .getAuthenticatedUser()
                .flatMapMany(user ->
                        this.libraryRepository.findAllAccessibleTo(user.subject()));
    }

    public Mono<MediaLibrary> findLibrary(Long libraryId) {
        return this.libraryRepository
                .findById(libraryId)
                .filter(MediaLibrary::isEnabled)
                .switchIfEmpty(Mono.error(new EntityNotFound("Library not found: " + libraryId)));
    }

    public Flux<DiscoveredFile> scanLibrary(Long libraryId) {
        return this.findLibrary(libraryId)
                .flatMapMany(
                        library -> this.libraryScanner
                                .scan(library.getRootPath())
                                .doOnComplete(
                                        () -> log.info(
                                                "Library scanned: id={} root={}",
                                                libraryId, library.getRootPath())));
    }
}