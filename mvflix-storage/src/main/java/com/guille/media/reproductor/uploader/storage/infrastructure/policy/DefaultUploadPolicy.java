package com.guille.media.reproductor.uploader.storage.infrastructure.policy;

import com.guille.media.reproductor.uploader.storage.app.errors.UploadSizeExceededException;
import com.guille.media.reproductor.uploader.storage.domain.models.UploadConfiguration;
import com.guille.media.reproductor.uploader.storage.domain.models.UploadType;
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
  private static final long STANDARD_MAX_UPLOAD_SIZE = 10L * GB;
  private static final long FREE_MAX_UPLOAD_SIZE = 500L * MB;

  private static final long MULTIPART_THRESHOLD = 500L * MB;

  @Override
  public UploadConfiguration resolve(long size, MimeType mimeType) {
    validateSize(size);
    MimeType validatedMimetype = validateMimeType(mimeType);

    if (size >= MULTIPART_THRESHOLD) {
      return new UploadConfiguration(
          Duration.ofHours(6), UploadType.MULTIPART, 50L * MB, validatedMimetype);
    }

    return new UploadConfiguration(
        Duration.ofMinutes(30), UploadType.SIMPLE, null, validatedMimetype);
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
