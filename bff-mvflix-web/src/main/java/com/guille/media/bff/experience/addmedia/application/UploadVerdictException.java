package com.guille.media.bff.experience.addmedia.application;

/**
 * Veredicto no transitorio del storage: la subida es inconsistente o falló de forma
 * definitiva; el orquestador ejecuta rollback y, según el caso, penalidad.
 */
public class UploadVerdictException extends RuntimeException {

  private final String code;

  public UploadVerdictException(String code, String message) {
    super(message);
    this.code = code;
  }

  public String getCode() {
    return this.code;
  }
}
