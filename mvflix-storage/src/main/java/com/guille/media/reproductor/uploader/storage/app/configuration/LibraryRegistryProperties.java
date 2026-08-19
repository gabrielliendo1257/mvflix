package com.guille.media.reproductor.uploader.storage.app.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Configuración del media server: raíces del filesystem permitidas para
 * bibliotecas. Un usuario solo puede registrar un path que quede bajo alguna
 * de estas raíces (frontera de seguridad del escaneo runtime). Con
 * {@code storage.allow-any-root: true} (dev/USB, etc.) se acepta cualquier
 * directorio y la lista de raíces se ignora.
 */
@Component
@ConfigurationProperties(prefix = "storage")
public class LibraryRegistryProperties {

    private List<String> allowedRoots = List.of();
    private boolean allowAnyRoot = false;

    public List<String> getAllowedRoots() {
        return this.allowedRoots;
    }

    public void setAllowedRoots(List<String> allowedRoots) {
        this.allowedRoots = allowedRoots == null ? List.of() : allowedRoots;
    }

    public boolean isAllowAnyRoot() {
        return this.allowAnyRoot;
    }

    public void setAllowAnyRoot(boolean allowAnyRoot) {
        this.allowAnyRoot = allowAnyRoot;
    }
}