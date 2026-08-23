package com.guille.media.reproductor.uploader.storage.managedstorage.domain.port;

import com.guille.media.reproductor.uploader.storage.managedstorage.domain.model.UserStorage;

import reactor.core.publisher.Mono;

public interface UserStorageRepository {
	Mono<UserStorage> save(UserStorage user);

	Mono<UserStorage> findById(Long userStorageId);

	Mono<UserStorage> findByOwnerUsername(String ownerUsername);

	/**
	 * Consume storage de forma atómica. Devuelve el número de filas afectadas:
	 * 1 si la cuota se pudo reservar, 0 si la excedería.
	 */
	Mono<Long> consumeStorage(String ownerUsername, long bytes);

	/**
	 * Libera el storage reservado previamente (sesión fallida o expirada).
	 * Nunca baja de cero.
	 */
	Mono<Long> releaseStorage(String ownerUsername, long bytes);
}