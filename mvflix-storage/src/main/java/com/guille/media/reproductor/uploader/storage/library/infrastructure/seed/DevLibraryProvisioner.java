package com.guille.media.reproductor.uploader.storage.library.infrastructure.seed;

import com.guille.media.reproductor.uploader.storage.library.infrastructure.LibraryRegistryProperties;
import com.guille.media.reproductor.uploader.storage.library.domain.model.MediaLibrary;
import com.guille.media.reproductor.uploader.storage.library.domain.model.MediaLibraryType;
import com.guille.media.reproductor.uploader.storage.library.domain.port.MediaLibraryRepository;

import lombok.extern.slf4j.Slf4j;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;

/**
 * Seed de entorno dev: registra las bibliotecas del operador definidas en
 * {@code storage.allowed-roots} (application-dev.yml) y crea los directorios
 * si no existen. Idempotente (upsert por root_path). Esas mismas raíces son la
 * frontera de seguridad para las bibliotecas registradas en runtime.
 */
@Slf4j
@Component
@Profile("dev")
public class DevLibraryProvisioner implements ApplicationRunner {

    private static final Duration TIMEOUT = Duration.ofSeconds(30);

    private final MediaLibraryRepository libraryRepository;
    private final LibraryRegistryProperties properties;

    public DevLibraryProvisioner(
            MediaLibraryRepository libraryRepository, LibraryRegistryProperties properties) {
        this.libraryRepository = libraryRepository;
        this.properties = properties;
    }

    @Override
    public void run(ApplicationArguments args) {
        List<String> roots = this.properties.getAllowedRoots();
        if (roots.isEmpty()) {
            log.warn("Dev library: storage.allowed-roots vacio, no se provisionan bibliotecas");
            return;
        }
        for (String root : roots) {
            try {
                Path dir = Path.of(root);
                if (!Files.isDirectory(dir)) {
                    Files.createDirectories(dir);
                }
                this.libraryRepository
                        .findByRootPath(root)
                        .switchIfEmpty(
                                this.libraryRepository.save(
                                        MediaLibrary.create(MediaLibraryType.LOCAL, root)))
                        .block(TIMEOUT);
                log.info("Dev library provisioned: root={}", root);
            } catch (Exception error) {
                log.warn(
                        "Dev library could not be provisioned: root={} cause={}",
                        root, error.getMessage());
            }
        }
    }
}