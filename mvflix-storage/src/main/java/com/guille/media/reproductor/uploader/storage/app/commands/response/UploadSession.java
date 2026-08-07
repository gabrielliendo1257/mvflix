package com.guille.media.reproductor.uploader.storage.app.commands.response;

import com.guille.media.reproductor.uploader.storage.domain.models.ExpectedObjectData;
import com.guille.media.reproductor.uploader.storage.domain.models.StoreObject.StorageSessionStatus;
import com.guille.media.reproductor.uploader.storage.domain.vos.StorageKey;
import java.time.Instant;

public record UploadSession(
		String uploadId,
		String uploadUrl,
		StorageKey storageKey,
		String method,
		Instant expiresAt,
		StorageSessionStatus currentStatus,
		ExpectedObjectData objectData) {

}
