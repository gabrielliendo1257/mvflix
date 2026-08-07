package com.guille.media.reproductor.uploader.storage.domain.service;

import com.guille.media.reproductor.uploader.storage.app.commands.requets.CreateUploadCommand;
import com.guille.media.reproductor.uploader.storage.app.commands.requets.StreamingCommand;
import com.guille.media.reproductor.uploader.storage.app.commands.response.StreamingSession;
import com.guille.media.reproductor.uploader.storage.app.commands.response.UploadSession;

import reactor.core.publisher.Mono;

public interface StorageService {
	Mono<UploadSession> createUploadSession(CreateUploadCommand command);

	Mono<StreamingSession> generateStreamingSession(StreamingCommand command);

	Mono<Void> completeUpload(Long uploadId);
}
