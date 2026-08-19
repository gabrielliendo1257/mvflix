package com.guille.media.reproductor.uploader.storage.infrastructure.library;

import com.guille.media.reproductor.uploader.storage.domain.exceptions.LibraryRootUnavailableException;
import com.guille.media.reproductor.uploader.storage.domain.ports.LibraryScanner;
import com.guille.media.reproductor.uploader.storage.domain.vos.DiscoveredFile;

import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Component;

import reactor.core.publisher.Flux;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Scanner de bibliotecas LOCAL: camina el root y reporta archivos multimedia.
 *
 * <p>Frontera anti path-traversal: todo archivo se resuelve con {@code toRealPath()} y se
 * descarta si no cuelga del root real. Los symlinks que apunten fuera del root quedan fuera.
 *
 * <p>Si el root no es accesible (USB desmontado, carpeta borrada) el escaneo falla con
 * {@link LibraryRootUnavailableException} en lugar de devolver un vacio silencioso; si el
 * root desaparece a mitad del recorrido se devuelve lo ya descubierto y se registra el warn.
 */
@Slf4j
@Component
public class FilesystemLibraryScanner implements LibraryScanner {

    private static final int MAX_DEPTH = 32;

    private static final Set<String> SUPPORTED_EXTENSIONS =
            Set.of("mkv", "mp4", "avi", "mov", "webm");

    private static final Set<String> PARTIAL_DOWNLOAD_SUFFIXES =
            Set.of(".part", ".crdownload", ".download", ".opdownload", ".tmp", ".~", "!qB");

    private static final Map<String, String> MIME_BY_EXTENSION =
            Map.of(
                    "mkv", "video/x-matroska",
                    "mp4", "video/mp4",
                    "avi", "video/x-msvideo",
                    "mov", "video/quicktime",
                    "webm", "video/webm");

    @Override
    public Flux<DiscoveredFile> scan(String rootPath) {
        return Flux.defer(() -> Flux.fromIterable(this.scanBlocking(rootPath)));
    }

    private List<DiscoveredFile> scanBlocking(String rootPath) {
        Path root;
        try {
            root = Path.of(rootPath).toAbsolutePath().normalize();
            if (!Files.isDirectory(root)) {
                throw new LibraryRootUnavailableException(
                        "Raíz de biblioteca no disponible: " + rootPath);
            }
            root = root.toRealPath();
        } catch (IOException error) {
            throw new LibraryRootUnavailableException(
                    "Raíz de biblioteca inaccesible: " + rootPath + " (" + error.getMessage() + ")");
        }

        final Path realRoot = root;
        List<DiscoveredFile> discovered = new ArrayList<>();
        try (var paths = Files.walk(root, MAX_DEPTH)) {
            paths.filter(Files::isRegularFile)
                    .filter(path -> !this.isHidden(path))
                    .filter(path -> !this.isPartialDownload(path))
                    .filter(this::isSupported)
                    .forEach(
                            path -> {
                                try {
                                    Path real = path.toRealPath();
                                    if (!real.startsWith(realRoot)) {
                                        log.warn(
                                                "Archivo fuera del root de la biblioteca, "
                                                        + "descartado: {}",
                                                real);
                                        return;
                                    }
                                    discovered.add(
                                            new DiscoveredFile(
                                                    realRoot
                                                            .relativize(real)
                                                            .toString()
                                                            .replace('\\', '/'),
                                                    Files.size(real),
                                                    this.mimeTypeOf(real)));
                                } catch (IOException error) {
                                    log.warn(
                                            "Archivo no pudo inspeccionarse, descartado: {}",
                                            path);
                                }
                            });
        } catch (IOException | UncheckedIOException error) {
            log.warn("Error escaneando biblioteca: root={} cause={}", root, error.getMessage());
        }
        return discovered;
    }

    private boolean isHidden(Path path) {
        return path.getFileName() != null && path.getFileName().toString().startsWith(".");
    }

    private boolean isPartialDownload(Path path) {
        if (path.getFileName() == null) {
            return false;
        }
        String name = path.getFileName().toString();
        return PARTIAL_DOWNLOAD_SUFFIXES.stream()
                .anyMatch(suffix -> name.endsWith(suffix));
    }

    private boolean isSupported(Path path) {
        String name = path.getFileName() == null ? "" : path.getFileName().toString();
        int dot = name.lastIndexOf('.');
        return dot >= 0 && SUPPORTED_EXTENSIONS.contains(name.substring(dot + 1).toLowerCase());
    }

    private String mimeTypeOf(Path path) {
        String name = path.getFileName().toString();
        int dot = name.lastIndexOf('.');
        String extension = name.substring(dot + 1).toLowerCase();
        return MIME_BY_EXTENSION.getOrDefault(extension, "application/octet-stream");
    }
}