package com.guille.media.reproductor.uploader.storage.library.application;

import com.guille.media.reproductor.uploader.storage.shared.security.UserProvider;
import com.guille.media.reproductor.uploader.storage.library.domain.exception.LibraryAccessDeniedException;
import com.guille.media.reproductor.uploader.storage.library.domain.exception.LibraryAlreadyExistsException;
import com.guille.media.reproductor.uploader.storage.library.domain.model.MediaLibrary;
import com.guille.media.reproductor.uploader.storage.library.domain.model.MediaLibraryType;
import com.guille.media.reproductor.uploader.storage.library.domain.port.LibraryRootResolver;
import com.guille.media.reproductor.uploader.storage.library.domain.port.MediaLibraryRepository;

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

    /**
     * Política de producto: las bibliotecas locales son del OPERADOR. Solo un
     * admin puede registrarlas; los usuarios normales cargan contenido por el
     * camino MANAGED (uploads a MinIO). La autorización corre antes de tocar
     * filesystem: no se valida nada para quien ya sabe que no puede.
     */
    public Mono<MediaLibrary> execute(String rootPath) {
        return this.userProvider
                .getAuthenticatedUser()
                .flatMap(user -> {
                    if (!user.isAdmin()) {
                        return Mono.error(new LibraryAccessDeniedException(
                                "Solo un administrador puede registrar bibliotecas locales"));
                    }
                    return this.registerOwned(user.subject(), rootPath);
                });
    }

    private Mono<MediaLibrary> registerOwned(String ownerSubject, String rootPath) {
        return Mono.fromCallable(() -> {
                    String realPath = this.rootResolver.resolveRealPath(rootPath);
                    this.rootResolver.assertAllowed(realPath);
                    return realPath;
                })
                .subscribeOn(Schedulers.boundedElastic())
                .flatMap(realPath -> this.libraryRepository
                        .findByRootPath(realPath)
                        .flatMap(existing -> existing.isOwnedBy(ownerSubject)
                                ? Mono.just(existing)
                                : Mono.error(new LibraryAlreadyExistsException(
                                        "Library already registered: " + realPath)))
                        .switchIfEmpty(Mono.defer(() -> this.libraryRepository.save(
                                MediaLibrary.createOwned(
                                        MediaLibraryType.LOCAL, realPath, ownerSubject)))))
                        .doOnNext(library -> log.info(
                                "Library registrada: id={} root={} owner={}",
                                library.getId(), library.getRootPath(),
                                library.getOwnerUsername()));
    }
}
