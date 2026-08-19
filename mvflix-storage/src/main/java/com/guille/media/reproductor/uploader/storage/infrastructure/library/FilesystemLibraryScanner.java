package com.guille.media.reproductor.uploader.storage.infrastructure.library;

import com.guille.media.reproductor.uploader.storage.domain.exceptions.LibraryRootUnavailableException;
import com.guille.media.reproductor.uploader.storage.domain.exceptions.ScanLimitExceededException;
import com.guille.media.reproductor.uploader.storage.domain.ports.LibraryScanner;
import com.guille.media.reproductor.uploader.storage.domain.vos.DiscoveredFile;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Iterator;
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
 *
 * <p>El recorrido corre en {@code boundedElastic} para no bloquear el event loop, es
 * cancelable de forma cooperativa ({@link ScanCancellationRegistry}) y aborta con
 * {@link ScanLimitExceededException} si supera {@code storage.scan-max-files} archivos.
 */
@Slf4j
@Component
public class FilesystemLibraryScanner implements LibraryScanner {

    private static final int MAX_DEPTH = 32;

    private static final int CANCEL_CHECK_EVERY = 200;

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

    private final ScanCancellationRegistry cancellationRegistry;
    private final int maxFiles;

    public FilesystemLibraryScanner(
            ScanCancellationRegistry cancellationRegistry,
            @Value("${storage.scan-max-files:20000}") int maxFiles) {
        this.cancellationRegistry = cancellationRegistry;
        this.maxFiles = maxFiles;
    }

    @Override
    public Flux<DiscoveredFile> scan(String rootPath) {
        return Flux.defer(() -> Flux.fromIterable(this.scanBlocking(rootPath)))
                .subscribeOn(Schedulers.boundedElastic());
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

        try {
            return this.walk(root);
        } finally {
            this.cancellationRegistry.clear(root.toString());
        }
    }

    private List<DiscoveredFile> walk(Path root) {
        final Path realRoot;
        try {
            realRoot = root.toRealPath();
        } catch (IOException error) {
            throw new LibraryRootUnavailableException(
                    "Raíz de biblioteca inaccesible: " + root + " (" + error.getMessage() + ")");
        }

        List<DiscoveredFile> discovered = new ArrayList<>();
        try (var paths = Files.walk(realRoot, MAX_DEPTH)) {
            Iterator<Path> iterator = paths.iterator();
            int visited = 0;
            while (iterator.hasNext()) {
                Path path = iterator.next();
                visited++;
                if (visited % this.CANCEL_CHECK_EVERY == 0
                        && this.cancellationRegistry.isCancelled(realRoot.toString())) {
                    log.warn(
                            "Escaneo cancelado por el usuario: root={} descubiertos={}",
                            realRoot, discovered.size());
                    return discovered;
                }
                if (discovered.size() >= this.maxFiles) {
                    throw new ScanLimitExceededException(
                            "Escaneo abortado: límite de " + this.maxFiles
                                    + " archivos superado en " + realRoot);
                }
                if (!Files.isRegularFile(path)
                        || this.isHidden(path)
                        || this.isPartialDownload(path)
                        || !this.isSupported(path)) {
                    continue;
                }
                try {
                    Path real = path.toRealPath();
                    if (!real.startsWith(realRoot)) {
                        log.warn(
                                "Archivo fuera del root de la biblioteca, descartado: {}",
                                real);
                        continue;
                    }
                    discovered.add(
                            new DiscoveredFile(
                                    realRoot.relativize(real).toString().replace('\\', '/'),
                                    Files.size(real),
                                    this.mimeTypeOf(real)));
                } catch (IOException error) {
                    log.warn("Archivo no pudo inspeccionarse, descartado: {}", path);
                }
            }
        } catch (IOException | UncheckedIOException error) {
            log.warn("Error escaneando biblioteca: root={} cause={}", realRoot, error.getMessage());
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