package com.guille.media.reproductor.uploader.storage.presenter.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.guille.media.reproductor.uploader.storage.domain.models.ExpectedObjectData;

public record UploadResponse(
		@JsonProperty(value = "uploadId") String uploadId,
		@JsonProperty(value = "uploadUrl") String uploadUrl,
		@JsonProperty(value = "storageKey") String storageKey,
		@JsonProperty String method,
		@JsonProperty(value = "object") ExpectedObjectData objectData) {

}
