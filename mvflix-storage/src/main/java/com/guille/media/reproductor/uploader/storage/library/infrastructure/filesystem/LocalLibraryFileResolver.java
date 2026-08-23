package com.guille.media.reproductor.uploader.storage.library.infrastructure.filesystem;

import com.guille.media.reproductor.uploader.storage.library.domain.model.MediaLibrary;
import com.guille.media.reproductor.uploader.storage.library.domain.port.LibraryContentResolver;
import com.guille.media.reproductor.uploader.storage.library.domain.model.LibraryFileHandle;

import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Component;

import reactor.core.publisher.Mono;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Resuelve un archivo de biblioteca LOCAL garantizando que queda dentro del
 * root real (anti path-traversal): normalize + toRealPath + startsWith.
 */
@Slf4j
@Component
public class LocalLibraryFileResolver implements LibraryContentResolver {

    @Override
    public Mono<LibraryFileHandle> resolve(MediaLibrary library, String relativePath) {
        return Mono.fromCallable(() -> {
            Path root;
            try {
                root = Path.of(library.getRootPath()).toAbsolutePath().normalize().toRealPath();
            } catch (IOException error) {
                log.warn("Root de biblioteca inaccesible: {} cause={}",
                        library.getRootPath(), error.getMessage());
                return null;
            }

            Path candidate = root.resolve(relativePath).normalize();
            if (!candidate.startsWith(root)) {
                log.warn("Ruta fuera del root de la biblioteca, rechazada: {}", relativePath);
                return null;
            }

            Path real;
            try {
                real = candidate.toRealPath();
            } catch (IOException error) {
                return null;
            }
            if (!real.startsWith(root) || !Files.isRegularFile(real)) {
                return null;
            }
            return new LibraryFileHandle(
                    root.relativize(real).toString().replace('\\', '/'),
                    real,
                    Files.size(real),
                    mimeTypeOf(real));
        });
    }

    private String mimeTypeOf(Path path) {
        String name = path.getFileName().toString();
        int dot = name.lastIndexOf('.');
        String extension = dot >= 0 ? name.substring(dot + 1).toLowerCase() : "";
        return switch (extension) {
            case "mkv" -> "video/x-matroska";
            case "mp4" -> "video/mp4";
            case "avi" -> "video/x-msvideo";
            case "mov" -> "video/quicktime";
            case "webm" -> "video/webm";
            default -> "application/octet-stream";
        };
    }
}