package com.guille.media.reproductor.uploader.storage.managedstorage.domain.exception;

import com.guille.media.reproductor.uploader.storage.managedstorage.domain.model.MimeType;

import lombok.Getter;

@Getter
public class UnsupportedMimeTypeException extends RuntimeException {
    private MimeType mimeType;

    public UnsupportedMimeTypeException(MimeType mimeType) {
        super();
        this.mimeType = mimeType;
    }
}
