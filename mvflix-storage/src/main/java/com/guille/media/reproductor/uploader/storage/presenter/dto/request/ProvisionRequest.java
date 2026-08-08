package com.guille.media.reproductor.uploader.storage.presenter.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ProvisionRequest(
		@JsonProperty(value = "quota_bytes") Long quotaBytes) {

}