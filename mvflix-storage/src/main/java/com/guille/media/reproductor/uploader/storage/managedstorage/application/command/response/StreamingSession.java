package com.guille.media.reproductor.uploader.storage.managedstorage.application.command.response;

import com.guille.media.reproductor.uploader.storage.managedstorage.domain.model.StorageKey;
import java.time.Instant;

public record StreamingSession(
		String uploadId,
		String streamingUrl,
		StorageKey storageKey,
		Instant expiresAt,
		String method) {

}
