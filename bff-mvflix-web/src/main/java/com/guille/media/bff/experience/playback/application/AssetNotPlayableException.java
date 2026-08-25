package com.guille.media.bff.experience.playback.application;

/**
 * La media existe y es visible, pero hoy no tiene contenido reproducible.
 * Códigos: {@code MEDIA_NOT_READY} (aún DRAFT), {@code NO_PLAYABLE_ASSET}
 * (sin asset identificado). HTTP 409: estado del dominio, no falta de permisos.
 */
public class AssetNotPlayableException extends RuntimeException {

  private final String code;

  public AssetNotPlayableException(String code, String message) {
    super(message);
    this.code = code;
  }

  public String getCode() {
    return this.code;
  }
}
