package com.guille.media.bff.experience.addmedia.application;

/**
 * Rechazo 4xx de un servicio aguas abajo (p.ej. 404/409): conserva el status
 * para que la aplicación decida (reconciliar, rollback, propagar).
 */
public class DownstreamRejectionException extends RuntimeException {

  private final int status;

  public DownstreamRejectionException(int status, String message) {
    super(message);
    this.status = status;
  }

  public int status() {
    return this.status;
  }
}
