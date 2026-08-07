package com.guille.media.reproductor.uploader.storage.domain.service;

import com.guille.media.reproductor.uploader.storage.domain.models.UploadConfiguration;
import com.guille.media.reproductor.uploader.storage.domain.vos.MimeType;

public interface UploadPolicy {
    UploadConfiguration resolve(long size, MimeType mimeType);

    long maxUploadSize();

    boolean supports(MimeType mimeType);
}
