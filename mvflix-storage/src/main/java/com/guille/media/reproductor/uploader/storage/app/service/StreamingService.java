package com.guille.media.reproductor.uploader.storage.app.service;

import com.guille.media.reproductor.uploader.storage.app.commands.requests.StreamingCommand;
import com.guille.media.reproductor.uploader.storage.app.commands.response.StreamingSession;

import reactor.core.publisher.Mono;

/** Consumo del contenido: genera sesiones de streaming para objetos disponibles. */
public interface StreamingService {

  /**
   * Preview exclusivo del PROPIETARIO del objeto. Cuando exista catálogo con
   * visibilidad pública/compartida, ese playback NO pasa por aquí sino por
   * {@link #generateCatalogStreamingSession}, previa validación de Movies.
   */
  Mono<StreamingSession> generateStreamingSession(StreamingCommand command);

  /**
   * Playback M2M del catálogo: lo invoca un servicio autorizado (movies/BFF)
   * que YA validó visibilidad (PUBLIC/SHARED/owner). Storage solo comprueba
   * que quien pide bytes tiene el scope de reproducción y que el objeto está
   * disponible; no conoce conceptos de catálogo.
   */
  Mono<StreamingSession> generateCatalogStreamingSession(StreamingCommand command);
}
