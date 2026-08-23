package com.guille.media.reproductor.uploader.storage.managedstorage.application.error;

import lombok.Getter;

@Getter
public class UploadSizeExceededException extends RuntimeException {
    private long size;
    private long maxSize;

    public UploadSizeExceededException(long size, long maxSize) {
        super();
        this.size = size;
        this.maxSize = maxSize;
    }
}
