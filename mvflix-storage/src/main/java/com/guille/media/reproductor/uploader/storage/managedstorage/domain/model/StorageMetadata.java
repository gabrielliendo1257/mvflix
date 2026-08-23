package com.guille.media.reproductor.uploader.storage.managedstorage.domain.model;

import java.time.Instant;

/**
 * Metadatos descriptivos de un objeto almacenado.
 *
 * <p>Este Value Object representa información técnica y descriptiva del archivo sin incluir su
 * contenido binario. Se utiliza para:
 *
 * <ul>
 *   <li>Validar tipos de archivo permitidos.
 *   <li>Mostrar tamaño y fechas al usuario.
 *   <li>Verificar integridad mediante checksums.
 *   <li>Tomar decisiones de negocio (por ejemplo, calcular cuotas).
 * </ul>
 *
 * <p>Ejemplo:
 *
 * <pre>
 * StorageMetadata metadata = new StorageMetadata(
 * 		"video/mp4",
 * 		2_147_483_648L,
 * 		"\"e4d909c290d0fb1ca068ffaddf22cbd0\"",
 * 		Instant.now(),
 * 		Map.of("resolution", "1920x1080"));
 * </pre>
 */
public record StorageMetadata(
        String contentType, long contentLength, String checksum, Instant lastModifiedAt) {

    /**
     * Crea una nueva instancia de metadatos.
     *
     * @param contentType tipo MIME del objeto.
     * @param contentLength tamaño en bytes.
     * @param checksum hash o ETag del proveedor.
     * @param lastModifiedAt fecha de última modificación.
     * @throws IllegalArgumentException si el tamaño es negativo.
     * @throws NullPointerException si algún parámetro obligatorio es nulo.
     */
    public StorageMetadata {
        if (contentLength < 0) {
            throw new IllegalArgumentException("Content length can not be negative");
        }
        // attributes = Map.copyOf(Objects.requireNonNullElse(attributes, Map.of()));
    }

    /**
     * Determina si el objeto es un video.
     *
     * @return true si el tipo MIME comienza con "video/".
     */
    public boolean isVideo() {
        return contentType.startsWith("video/");
    }

    /**
     * Determina si el objeto es una imagen.
     *
     * @return true si el tipo MIME comienza con "image/".
     */
    public boolean isImage() {
        return contentType.startsWith("image/");
    }

    /**
     * Determina si el objeto es un subtítulo.
     *
     * @return true si corresponde a formatos comunes de subtítulos.
     */
    public boolean isSubtitle() {
        return contentType.equals("text/vtt") || contentType.equals("application/x-subrip");
    }
}
