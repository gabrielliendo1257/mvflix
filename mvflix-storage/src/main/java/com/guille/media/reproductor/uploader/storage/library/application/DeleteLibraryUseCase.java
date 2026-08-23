package com.guille.media.reproductor.uploader.storage.library.application;

import com.guille.media.reproductor.uploader.storage.shared.security.UserProvider;
import com.guille.media.reproductor.uploader.storage.library.domain.exception.LibraryAccessDeniedException;
import com.guille.media.reproductor.uploader.storage.library.domain.port.MediaLibraryRepository;
import com.guille.media.reproductor.uploader.storage.shared.error.EntityNotFound;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;

import reactor.core.publisher.Mono;

/**
 * Elimina una biblioteca registrada en runtime. Solo el dueño puede borrarla
 * (las del operador no se borran por API).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DeleteLibraryUseCase {

    private final MediaLibraryRepository libraryRepository;
    private final UserProvider userProvider;

    public Mono<Void> execute(Long libraryId) {
        return this.userProvider
                .getAuthenticatedUser()
                .flatMap(user -> this.libraryRepository
                        .findById(libraryId)
                        .switchIfEmpty(Mono.error(new EntityNotFound(
                                "Library not found: " + libraryId)))
                        .filter(library -> library.isOwnedBy(user.subject()))
                        .switchIfEmpty(Mono.error(new LibraryAccessDeniedException(
                                "Library not owned: " + libraryId)))
                        .flatMap(library -> this.libraryRepository.deleteById(libraryId)))
                .doOnSuccess(v -> log.info("Library eliminada: id={}", libraryId));
    }
}