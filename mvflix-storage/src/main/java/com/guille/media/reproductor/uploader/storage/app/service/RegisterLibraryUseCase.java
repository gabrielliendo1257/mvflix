package com.guille.media.reproductor.uploader.storage.app.service;

import com.guille.media.reproductor.uploader.storage.app.configuration.LibraryRegistryProperties;
import com.guille.media.reproductor.uploader.storage.app.security.UserProvider;
import com.guille.media.reproductor.uploader.storage.domain.exceptions.LibraryAlreadyExistsException;
import com.guille.media.reproductor.uploader.storage.domain.exceptions.LibraryPathInvalidException;
import com.guille.media.reproductor.uploader.storage.domain.exceptions.LibraryPathNotAllowedException;
import com.guille.media.reproductor.uploader.storage.domain.models.MediaLibrary;
import com.guille.media.reproductor.uploader.storage.domain.models.MediaLibraryType;
import com.guille.media.reproductor.uploader.storage.domain.ports.MediaLibraryRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;

import reactor.core.publisher.Mono;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Registra una biblioteca en runtime desde un path del filesystem (elegido en
 * la UI). Valida que el directorio exista y que quede bajo una raíz permitida
 * ({@code storage.allowed-roots}); la biblioteca queda a nombre del usuario
 * autenticado.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RegisterLibraryUseCase {

    private final MediaLibraryRepository libraryRepository;
    private final UserProvider userProvider;
    private final LibraryRegistryProperties properties;

    public Mono<MediaLibrary> execute(String rootPath) {
        String realPath = this.resolveRealPath(rootPath);
        this.assertAllowed(realPath);
        return this.userProvider
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
                        library.getId(), library.getRootPath(), library.getOwnerUsername()));
    }

    private String resolveRealPath(String rootPath) {
        if (rootPath == null || rootPath.isBlank()) {
            throw new LibraryPathInvalidException("Path vacío");
        }
        Path absolute = Path.of(rootPath.trim()).toAbsolutePath().normalize();
        if (!Files.isDirectory(absolute)) {
            throw new LibraryPathInvalidException(
                    "No es un directorio accesible: " + absolute);
        }
        try {
            return absolute.toRealPath().toString();
        } catch (IOException error) {
            throw new LibraryPathInvalidException(
                    "Directorio inaccesible: " + absolute + " (" + error.getMessage() + ")");
        }
    }

    private void assertAllowed(String realPath) {
        if (this.properties.isAllowAnyRoot()) {
            return;
        }
        List<Path> roots = this.properties.getAllowedRoots().stream()
                .map(root -> this.realPathOf(root))
                .toList();
        boolean allowed = roots.stream().anyMatch(root -> Path.of(realPath).startsWith(root));
        if (!allowed) {
            throw new LibraryPathNotAllowedException(
                    "Path fuera de las raíces permitidas: " + realPath);
        }
    }

    private Path realPathOf(String root) {
        try {
            return Path.of(root).toAbsolutePath().normalize().toRealPath();
        } catch (IOException error) {
            throw new LibraryPathInvalidException(
                    "Raíz permitida no accesible: " + root);
        }
    }
}