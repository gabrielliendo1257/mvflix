package com.guille.media.reproductor.uploader.storage.infrastructure.errors;

import com.guille.media.reproductor.uploader.storage.domain.vos.MimeType;

import lombok.Getter;

@Getter
public class UnsupportedMimeTypeException extends RuntimeException {
    private MimeType mimeType;

    public UnsupportedMimeTypeException(MimeType mimeType) {
        super();
        this.mimeType = mimeType;
    }
}
