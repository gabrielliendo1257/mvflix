package com.guille.media.reproductor.uploader.storage.domain.models;

import java.time.Duration;

/**
 * Configuración de una sesión de upload. Hoy solo hay un mecanismo real
 * (presigned PUT simple); cuando exista upload multipart/resumable, este
 * registro expresará la variante elegida.
 */
public record UploadConfiguration(Duration expiration) {

}
