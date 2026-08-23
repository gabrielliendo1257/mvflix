package com.guille.media.reproductor.uploader.storage.domain.models;

import java.time.Instant;

/**
 * Biblioteca del media server. Las del OPERADOR ({@code ownerUsername == null})
 * se configuran por entorno; las de un USUARIO final se registran en runtime
 * desde la UI. {@code root} es la frontera: todos los assets de la biblioteca
 * viven bajo ese directorio (o prefix S3) y el resto de la app solo conoce
 * {@code relativePath}.
 */
public class MediaLibrary {

    private final Long id;
    private final MediaLibraryType type;
    private final String rootPath;
    private final boolean enabled;
    private final String ownerUsername;
    private final Instant createdAt;

    public MediaLibrary(Long id, MediaLibraryType type, String rootPath, boolean enabled,
            String ownerUsername, Instant createdAt) {
        this.id = id;
        this.type = type;
        this.rootPath = rootPath;
        this.enabled = enabled;
        this.ownerUsername = ownerUsername;
        this.createdAt = createdAt;
    }

    public static MediaLibrary create(MediaLibraryType type, String rootPath) {
        return new MediaLibrary(null, type, rootPath, true, null, Instant.now());
    }

    public static MediaLibrary createOwned(MediaLibraryType type, String rootPath,
            String ownerUsername) {
        return new MediaLibrary(null, type, rootPath, true, ownerUsername, Instant.now());
    }

    public boolean isOwnedBy(String username) {
        return this.ownerUsername != null && this.ownerUsername.equals(username);
    }

    /**
     * Regla de visibilidad coherente con el listado accesible
     * ({@code owner IS NULL OR owner = :owner} y {@code enabled}): las
     * bibliotecas del operador son compartidas; las de usuario, privadas.
     * Protege scan/serving igual que el listado para no abrir huecos por ID.
     */
    public boolean isAccessibleTo(String username) {
        return this.enabled && (this.ownerUsername == null || this.ownerUsername.equals(username));
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

    public String getOwnerUsername() {
        return this.ownerUsername;
    }

    public Instant getCreatedAt() {
        return this.createdAt;
    }
}
