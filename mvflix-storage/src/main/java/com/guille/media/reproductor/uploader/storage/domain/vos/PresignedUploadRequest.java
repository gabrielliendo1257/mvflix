package com.guille.media.reproductor.uploader.storage.domain.vos;

import lombok.Getter;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@Getter
public class PresignedUploadRequest {
	private static final String HEADER_CONTENT_TYPE = "Content-Type";
    private final Duration expiration;
    private Map<String, String> headers = new HashMap<>();

    public PresignedUploadRequest(Duration expiration, Map<String, String> headers) {
        this.expiration = expiration;
        this.headers = headers;
    }

	public PresignedUploadRequest(Duration expiration) {
		this.expiration = expiration;
	}

	public void setContentType(MimeType mimeType) {
		this.headers.put(HEADER_CONTENT_TYPE, mimeType.value());
	}
}
