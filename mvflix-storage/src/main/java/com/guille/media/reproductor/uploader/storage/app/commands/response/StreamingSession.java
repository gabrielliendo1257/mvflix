package com.guille.media.reproductor.uploader.storage.app.commands.response;

import com.guille.media.reproductor.uploader.storage.domain.vos.StorageKey;
import java.time.Instant;

public record StreamingSession(
		String uploadId,
		String streamingUrl,
		StorageKey storageKey,
		Instant expiresAt,
		String method) {

}
