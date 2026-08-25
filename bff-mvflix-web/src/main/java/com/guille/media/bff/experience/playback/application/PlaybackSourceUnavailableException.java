package com.guille.media.bff.experience.playback.application;

/**
 * El contenido no está accesible ahora: storage caído, objeto desaparecido,
 * respuesta inválida de la capacidad de acceso. HTTP 503: para el usuario es
 * "no disponible en este momento"; la causa fina queda en logs/trazas.
 */
public class PlaybackSourceUnavailableException extends RuntimeException {

  public PlaybackSourceUnavailableException(String message, Throwable cause) {
    super(message, cause);
  }
}
