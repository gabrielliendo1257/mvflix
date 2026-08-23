package com.guille.media.reproductor.uploader.storage.managedstorage.domain.model;

public record MimeType(String value) {
	public static MimeType of(String value) {
		return new MimeType(value);
	}
}
