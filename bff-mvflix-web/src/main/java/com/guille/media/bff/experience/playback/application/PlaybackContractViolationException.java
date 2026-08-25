package com.guille.media.bff.experience.playback.application;

/**
 * El catálogo envió datos contradictorios sobre dónde vive el contenido
 * (p.ej. locator MANAGED y LOCAL a la vez): violación de contrato entre
 * servicios, no un estado del usuario ni una caída temporal. HTTP 502.
 */
public class PlaybackContractViolationException extends RuntimeException {

  public PlaybackContractViolationException(String message) {
    super(message);
  }
}
