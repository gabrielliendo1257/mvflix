package com.guille.media.reproductor.uploader.storage.domain.vos;

public record MimeType(String value) {
	public static MimeType of(String value) {
		return new MimeType(value);
	}
}
