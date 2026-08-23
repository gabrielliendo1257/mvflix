package com.guille.media.reproductor.uploader.storage.infrastructure.policy;

import com.guille.media.reproductor.uploader.storage.app.errors.UploadSizeExceededException;
import com.guille.media.reproductor.uploader.storage.domain.models.UploadConfiguration;
import com.guille.media.reproductor.uploader.storage.domain.service.UploadPolicy;
import com.guille.media.reproductor.uploader.storage.domain.vos.MimeType;
import com.guille.media.reproductor.uploader.storage.infrastructure.errors.UnsupportedMimeTypeException;
import java.time.Duration;
import org.springframework.stereotype.Service;

@Service
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
