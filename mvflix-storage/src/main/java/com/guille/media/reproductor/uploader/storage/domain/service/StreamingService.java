package com.guille.media.reproductor.uploader.storage.domain.service;

import com.guille.media.reproductor.uploader.storage.app.commands.requests.StreamingCommand;
import com.guille.media.reproductor.uploader.storage.app.commands.response.StreamingSession;

import reactor.core.publisher.Mono;

/** Consumo del contenido: genera sesiones de streaming para objetos disponibles. */
public interface StreamingService {
  Mono<StreamingSession> generateStreamingSession(StreamingCommand command);
}