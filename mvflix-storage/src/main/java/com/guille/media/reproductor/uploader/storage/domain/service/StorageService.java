package com.guille.media.reproductor.uploader.storage.domain.service;

import com.guille.media.reproductor.uploader.storage.app.commands.requests.CreateUploadCommand;
import com.guille.media.reproductor.uploader.storage.app.commands.requests.StreamingCommand;
import com.guille.media.reproductor.uploader.storage.app.commands.response.StreamingSession;
import com.guille.media.reproductor.uploader.storage.app.commands.response.UploadSession;

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
}
