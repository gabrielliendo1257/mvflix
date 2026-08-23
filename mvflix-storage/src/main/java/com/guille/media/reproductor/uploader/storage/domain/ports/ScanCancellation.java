package com.guille.media.reproductor.uploader.storage.domain.ports;

/**
 * Cancelación cooperativa de escaneos de biblioteca: un flag efímero por raíz
 * que el scanner consulta mientras recorre el árbol.
 */
public interface ScanCancellation {

    /** Marca la cancelación pendiente para la raíz indicada. */
    void cancel(String rootPath);

    /** Indica si hay una cancelación pendiente para la raíz indicada. */
    boolean isCancelled(String rootPath);

    /** Libera el estado de la raíz al finalizar el escaneo. */
    void clear(String rootPath);
}
