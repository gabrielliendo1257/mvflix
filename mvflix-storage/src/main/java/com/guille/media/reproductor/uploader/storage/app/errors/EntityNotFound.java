package com.guille.media.reproductor.uploader.storage.app.errors;

/**
 * Error de aplicación: el recurso solicitado no existe. Se traduce a 404 en la
 * capa de transporte.
 */
public class EntityNotFound extends RuntimeException {

	public EntityNotFound(String message) {
		super(message);
	}
}
