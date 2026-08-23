package com.guille.media.bff.experience.addmedia.application;

/**
 * Un servicio aguas abajo falló o no fue alcanzable: SIN rollback (el front
 * puede reintentar). La traducción desde WebClient ocurre en los adapters
 * HTTP; la aplicación solo conoce esta excepción propia.
 */
public class DownstreamUnavailableException extends RuntimeException {

  private final int upstreamStatus;
  private final String code;

  public DownstreamUnavailableException(int upstreamStatus, String code, String message) {
    super(message);
    this.upstreamStatus = upstreamStatus;
    this.code = code;
  }

  public int getUpstreamStatus() {
    return this.upstreamStatus;
  }

  public String getCode() {
    return this.code;
  }
}
