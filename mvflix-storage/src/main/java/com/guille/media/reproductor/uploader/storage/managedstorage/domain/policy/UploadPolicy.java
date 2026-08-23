package com.guille.media.reproductor.uploader.storage.managedstorage.domain.policy;

import com.guille.media.reproductor.uploader.storage.managedstorage.domain.model.UploadConfiguration;
import com.guille.media.reproductor.uploader.storage.managedstorage.domain.model.MimeType;

public interface UploadPolicy {
    UploadConfiguration resolve(long size, MimeType mimeType);

    long maxUploadSize();

    boolean supports(MimeType mimeType);
}
