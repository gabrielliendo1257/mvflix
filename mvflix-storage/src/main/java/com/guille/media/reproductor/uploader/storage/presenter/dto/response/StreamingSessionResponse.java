package com.guille.media.reproductor.uploader.storage.presenter.dto.response;

public record StreamingSessionResponse(
		String uploadId,
		String streamingUrl,
		String storageKey,
		String expiresAt,
		String method) {

}
