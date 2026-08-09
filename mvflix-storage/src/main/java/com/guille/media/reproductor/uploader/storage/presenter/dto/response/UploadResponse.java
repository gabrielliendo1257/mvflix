package com.guille.media.reproductor.uploader.storage.presenter.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.guille.media.reproductor.uploader.storage.domain.models.ExpectedObjectData;
import com.guille.media.reproductor.uploader.storage.domain.models.StoreObject.StorageSessionStatus;

public record UploadResponse(
		@JsonProperty(value = "uploadId") String uploadId,
		@JsonProperty(value = "uploadUrl") String uploadUrl,
		@JsonProperty(value = "storageKey") String storageKey,
		@JsonProperty String method,
		@JsonProperty(value = "status") StorageSessionStatus status,
		@JsonProperty(value = "object") ExpectedObjectData objectData) {

}
