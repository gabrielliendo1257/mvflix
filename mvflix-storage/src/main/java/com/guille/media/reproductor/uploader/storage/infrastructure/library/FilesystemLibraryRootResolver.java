package com.guille.media.reproductor.uploader.storage.infrastructure.library;

import com.guille.media.reproductor.uploader.storage.app.configuration.LibraryRegistryProperties;
import com.guille.media.reproductor.uploader.storage.domain.exceptions.LibraryPathInvalidException;
import com.guille.media.reproductor.uploader.storage.domain.exceptions.LibraryPathNotAllowedException;
import com.guille.media.reproductor.uploader.storage.domain.ports.LibraryRootResolver;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Resolución REAL sobre el filesystem del host. Extraída del caso de uso para
 * que la aplicación no conozca Files/Path; las llamadas son bloqueantes y el
 * caso de uso las aísla en boundedElastic.
 */
@Component
@RequiredArgsConstructor
public class FilesystemLibraryRootResolver implements LibraryRootResolver {

    private final LibraryRegistryProperties properties;

    @Override
    public String resolveRealPath(String rawPath) {
        if (rawPath == null || rawPath.isBlank()) {
            throw new LibraryPathInvalidException("Path vacío");
        }
        Path absolute = Path.of(rawPath.trim()).toAbsolutePath().normalize();
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

    @Override
    public void assertAllowed(String realPath) {
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
