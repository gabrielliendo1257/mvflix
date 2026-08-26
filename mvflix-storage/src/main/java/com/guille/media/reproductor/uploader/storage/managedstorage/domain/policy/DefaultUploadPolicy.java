package com.guille.media.reproductor.uploader.storage.managedstorage.domain.policy;

import com.guille.media.reproductor.uploader.storage.managedstorage.domain.exception.UploadSizeExceededException;
import com.guille.media.reproductor.uploader.storage.managedstorage.domain.exception.UnsupportedMimeTypeException;
import com.guille.media.reproductor.uploader.storage.managedstorage.domain.model.MimeType;
import com.guille.media.reproductor.uploader.storage.managedstorage.domain.model.UploadConfiguration;

import java.time.Duration;

/**
 * Política de ingesta del contexto: tamaños, MIME soportados y expiración de
 * la sesión. Reglas de DOMINIO puras (sin Spring); se registra como bean en
 * {@code ManagedStorageBeanConfiguration}.
 */
public class DefaultUploadPolicy implements UploadPolicy {

  private static final long KB = 1024L;
  private static final long MB = KB * 1024L;
  private static final long GB = MB * 1024L;

  private static final long MAX_UPLOAD_SIZE = 20L * GB;

  /**
   * Todas las sesiones usan un único presigned PUT: el protocolo multipart no
   * está implementado, así que declararlo sería mentir sobre las capacidades
   * reales. Reintroducir la variante multipart cuando uploads grandes
   * necesiten reanudación.
   */
  private static final Duration SIMPLE_UPLOAD_EXPIRATION = Duration.ofMinutes(30);

  @Override
  public UploadConfiguration resolve(long size, MimeType mimeType) {
    validateSize(size);
    validateMimeType(mimeType);
    return new UploadConfiguration(SIMPLE_UPLOAD_EXPIRATION);
  }

  @Override
  public long maxUploadSize() {
    return MAX_UPLOAD_SIZE;
  }

  /**
   * Política de formatos de subida: solo contenedores reproducibles
   * nativamente por todos los navegadores. El resto (MKV, AVI, MOV, ...)
   * entrará por el futuro servicio de transcodificación; ampliar esta lista
   * antes de que exista catálogo contenido no reproducible con DIRECT.
   */
  @Override
  public boolean supports(MimeType mimeType) {
    return switch (mimeType.value()) {
      case "image/png", "image/jpeg", "video/mp4" -> true;

      default -> false;
    };
  }

  private void validateSize(long size) {
    if (size <= 0) {
      throw new IllegalArgumentException("Upload size must be greater than zero");
    }

    if (size > MAX_UPLOAD_SIZE) {
      throw new UploadSizeExceededException(size, MAX_UPLOAD_SIZE);
    }
  }

  private MimeType validateMimeType(MimeType mimeType) {
    if (!supports(mimeType)) {
      throw new UnsupportedMimeTypeException(mimeType);
    }
    return mimeType;
  }
}
