package com.guille.media.reproductor.uploader.storage.domain.models;

import java.time.Instant;

/**
 * Biblioteca del media server, configurada por el OPERADOR (nunca por un usuario final).
 * {@code root} es la frontera: todos los assets de la biblioteca viven bajo ese directorio
 * (o prefix S3) y el resto de la app solo conoce {@code relativePath}.
 */
public class MediaLibrary {

    private final Long id;
    private final MediaLibraryType type;
    private final String rootPath;
    private final boolean enabled;
    private final Instant createdAt;

    public MediaLibrary(Long id, MediaLibraryType type, String rootPath, boolean enabled,
            Instant createdAt) {
        this.id = id;
        this.type = type;
        this.rootPath = rootPath;
        this.enabled = enabled;
        this.createdAt = createdAt;
    }

    public static MediaLibrary create(MediaLibraryType type, String rootPath) {
        return new MediaLibrary(null, type, rootPath, true, Instant.now());
    }

    public Long getId() {
        return this.id;
    }

    public MediaLibraryType getType() {
        return this.type;
    }

    public String getRootPath() {
        return this.rootPath;
    }

    public boolean isEnabled() {
        return this.enabled;
    }

    public Instant getCreatedAt() {
        return this.createdAt;
    }
}