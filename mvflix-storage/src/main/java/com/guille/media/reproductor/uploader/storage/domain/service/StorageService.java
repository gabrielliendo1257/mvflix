package com.guille.media.reproductor.uploader.storage.domain.service;

import com.guille.media.reproductor.uploader.storage.app.commands.requests.CreateUploadCommand;
import com.guille.media.reproductor.uploader.storage.app.commands.requests.StreamingCommand;
import com.guille.media.reproductor.uploader.storage.app.commands.response.StreamingSession;
import com.guille.media.reproductor.uploader.storage.app.commands.response.UploadSession;
import com.guille.media.reproductor.uploader.storage.domain.models.UserStorage;

import reactor.core.publisher.Mono;

import java.time.Instant;

public interface StorageService {
	Mono<UploadSession> createUploadSession(CreateUploadCommand command);

	Mono<StreamingSession> generateStreamingSession(StreamingCommand command);

	Mono<Void> completeUpload(Long uploadId);

	/**
	 * Expira las sesiones {@code PENDING} anteriores a {@code cutoff}, liberando
	 * la cuota reservada. Devuelve el número de sesiones expiradas.
	 */
	Mono<Long> expireStaleSessions(Instant cutoff);

	/**
	 * Devuelve el storage (cuota y uso) del usuario autenticado.
	 */
	Mono<UserStorage> getUserStorage();

	/**
	 * Elimina el objeto (y su blob) del que el usuario autenticado es dueño,
	 * liberando la cuota asociada.
	 */
	Mono<Void> deleteObject(Long storageId);

	/**
	 * Asegura que el usuario tiene su espacio en el bucket dedicado:
	 * crea la fila de {@code user_storage} si no existe y materializa las
	 * subcarpetas en el objeto storage. Idempotente. Se invoca al registrar
	 * o loguear el usuario.
	 */
	Mono<Void> ensureUserStorage(String username, long quotaBytes);
}
