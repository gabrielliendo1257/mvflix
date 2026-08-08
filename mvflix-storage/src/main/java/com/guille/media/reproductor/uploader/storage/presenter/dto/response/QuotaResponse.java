package com.guille.media.reproductor.uploader.storage.presenter.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

public record QuotaResponse(
		@JsonProperty(value = "ownerUsername") String ownerUsername,
		@JsonProperty(value = "bucketName") String bucketName,
		@JsonProperty(value = "quotaBytes") long quotaBytes,
		@JsonProperty(value = "usedBytes") long usedBytes,
		@JsonProperty(value = "remainingBytes") long remainingBytes) {

}