package com.guille.media.reproductor.uploader.storage.managedstorage.infrastructure.web;

public record StreamingSessionResponse(
		String uploadId,
		String streamingUrl,
		String storageKey,
		String expiresAt,
		String method) {

}
