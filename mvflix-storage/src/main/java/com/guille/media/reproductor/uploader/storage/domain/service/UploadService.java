package com.guille.media.reproductor.uploader.storage.domain.service;

import com.guille.media.reproductor.uploader.storage.app.commands.requests.CreateUploadCommand;
import com.guille.media.reproductor.uploader.storage.app.commands.response.UploadSession;

import reactor.core.publisher.Mono;

/** Ciclo de vida de la subida: reserva de cuota, URL presigned y cierre del upload. */
public interface UploadService {
  Mono<UploadSession> createUploadSession(CreateUploadCommand command);

  Mono<Void> completeUpload(Long uploadId);
}