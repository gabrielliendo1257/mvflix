package com.guille.media.reproductor.uploader.storage.infrastructure.library;

import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Registro de cancelaciones de escaneo: un flag efimero por raiz de biblioteca
 * que el scanner comprueba mientras camina el arbol. Un escaneo nuevo limpia la
 * cancelacion anterior; al terminar (complete o corte) tambien se limpia.
 */
@Slf4j
@Component
public class ScanCancellationRegistry implements com.guille.media.reproductor.uploader.storage.domain.ports.ScanCancellation {


    private final Map<String, AtomicBoolean> cancelled = new ConcurrentHashMap<>();

    public void cancel(String rootPath) {
        this.cancelled.computeIfAbsent(rootPath, path -> new AtomicBoolean()).set(true);
        log.info("Escaneo cancelado solicitado: root={}", rootPath);
    }

    public boolean isCancelled(String rootPath) {
        AtomicBoolean flag = this.cancelled.get(rootPath);
        return flag != null && flag.get();
    }

    public void clear(String rootPath) {
        AtomicBoolean flag = this.cancelled.remove(rootPath);
        if (flag != null && flag.get()) {
            log.info("Escaneo finalizado tras cancelacion: root={}", rootPath);
        }
    }
}