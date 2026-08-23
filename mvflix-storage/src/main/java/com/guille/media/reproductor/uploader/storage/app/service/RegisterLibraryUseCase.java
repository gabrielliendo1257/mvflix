package com.guille.media.reproductor.uploader.storage.app.service;

import com.guille.media.reproductor.uploader.storage.shared.security.UserProvider;
import com.guille.media.reproductor.uploader.storage.domain.exceptions.LibraryAlreadyExistsException;
import com.guille.media.reproductor.uploader.storage.domain.models.MediaLibrary;
import com.guille.media.reproductor.uploader.storage.domain.models.MediaLibraryType;
import com.guille.media.reproductor.uploader.storage.domain.ports.LibraryRootResolver;
import com.guille.media.reproductor.uploader.storage.domain.ports.MediaLibraryRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;

import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

/**
 * Registra una biblioteca en runtime desde un path del filesystem (elegido en
 * la UI). La resolución/validación del root vive en el puerto
 * {@link LibraryRootResolver}; aquí solo coordinación y autorización.
 *
 * <p>La validación de root es I/O bloqueante: se ejecuta en boundedElastic,
 * nunca en el event loop. Errores de validación llegan como onError (400/400
 * vía GlobalExceptionHandler), no como excepciones síncronas.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RegisterLibraryUseCase {

    private final MediaLibraryRepository libraryRepository;
    private final UserProvider userProvider;
    private final LibraryRootResolver rootResolver;

    public Mono<MediaLibrary> execute(String rootPath) {
        return Mono.fromCallable(() -> {
                    String realPath = this.rootResolver.resolveRealPath(rootPath);
                    this.rootResolver.assertAllowed(realPath);
                    return realPath;
                })
                .subscribeOn(Schedulers.boundedElastic())
                .flatMap(realPath -> this.userProvider
                        .getAuthenticatedUser()
                        .flatMap(user -> this.libraryRepository
                                .findByRootPath(realPath)
                                .flatMap(existing -> existing.isOwnedBy(user.subject())
                                        ? Mono.just(existing)
                                        : Mono.error(new LibraryAlreadyExistsException(
                                                "Library already registered: " + realPath)))
                                .switchIfEmpty(Mono.defer(() -> this.libraryRepository.save(
                                        MediaLibrary.createOwned(
                                                MediaLibraryType.LOCAL, realPath, user.subject())))))
                        .doOnNext(library -> log.info(
                                "Library registrada: id={} root={} owner={}",
                                library.getId(), library.getRootPath(),
                                library.getOwnerUsername())));
    }
}
