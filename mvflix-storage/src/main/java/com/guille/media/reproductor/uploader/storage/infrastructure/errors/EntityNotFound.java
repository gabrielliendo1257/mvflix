package com.guille.media.reproductor.uploader.storage.infrastructure.errors;

public class EntityNotFound extends RuntimeException {

	public EntityNotFound(String message) {
		super(message);
	}
}
