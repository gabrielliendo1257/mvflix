package com.guille.media.reproductor.uploader.storage.library.domain.port;

/**
 * Validación y resolución de raíces de bibliotecas locales. Aísla el
 * filesystem del caso de uso de registro:
 *
 * <ul>
 *   <li>{@link #resolveRealPath}: normaliza, comprueba que sea directorio y
 *       colapsa symlinks al path real.</li>
 *   <li>{@link #assertAllowed}: verifica que el path real quede bajo alguna
 *       raíz permitida por la configuración del operador.</li>
 * </ul>
 */
public interface LibraryRootResolver {

    /** @throws LibraryPathInvalidException si el path es inválido o inaccesible. */
    String resolveRealPath(String rawPath);

    /** @throws LibraryPathNotAllowedException si el path queda fuera de las raíces permitidas. */
    void assertAllowed(String realPath);
}
