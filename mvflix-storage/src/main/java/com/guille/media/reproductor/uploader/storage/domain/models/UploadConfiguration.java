package com.guille.media.reproductor.uploader.storage.domain.models;

import com.guille.media.reproductor.uploader.storage.domain.vos.MimeType;

import java.time.Duration;

public record UploadConfiguration(
		Duration expiration,
		UploadType uploadType,
		Long chunkSize,
		MimeType mimeType) {

}
