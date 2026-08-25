package com.guille.media.bff.experience.playback.application;

/**
 * Capability de stream LOCAL inválida, expirada o usada en otro asset.
 * El reproductor debe pedir una sesión nueva. HTTP 401.
 */
public class LocalStreamTokenException extends RuntimeException {

  public LocalStreamTokenException(String message) {
    super(message);
  }
}
