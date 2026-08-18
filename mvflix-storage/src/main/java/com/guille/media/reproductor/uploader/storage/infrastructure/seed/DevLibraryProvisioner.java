package com.guille.media.reproductor.uploader.storage.infrastructure.seed;

import com.guille.media.reproductor.uploader.storage.domain.models.MediaLibrary;
import com.guille.media.reproductor.uploader.storage.domain.models.MediaLibraryType;
import com.guille.media.reproductor.uploader.storage.domain.ports.MediaLibraryRepository;

import lombok.extern.slf4j.Slf4j;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;

/**
 * Seed de entorno dev: registra las bibliotecas del operador definidas en
 * {@code storage.library-roots} (application-dev.yml) y crea los directorios
 * si no existen. Idempotente (upsert por root_path).
 */
@Slf4j
@Component
@Profile("dev")
@ConfigurationProperties(prefix = "storage")
public class DevLibraryProvisioner implements ApplicationRunner {

    private static final Duration TIMEOUT = Duration.ofSeconds(30);

    private final MediaLibraryRepository libraryRepository;

    private List<String> libraryRoots = List.of();

    public DevLibraryProvisioner(MediaLibraryRepository libraryRepository) {
        this.libraryRepository = libraryRepository;
    }

    public List<String> getLibraryRoots() {
        return this.libraryRoots;
    }

    public void setLibraryRoots(List<String> libraryRoots) {
        this.libraryRoots = libraryRoots == null ? List.of() : libraryRoots;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (this.libraryRoots.isEmpty()) {
            log.warn("Dev library: storage.library-roots vacio, no se provisionan bibliotecas");
            return;
        }
        for (String root : this.libraryRoots) {
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