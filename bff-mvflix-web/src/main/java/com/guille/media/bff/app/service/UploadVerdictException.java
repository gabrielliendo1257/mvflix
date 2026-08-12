package com.guille.media.bff.app.service;

/**
 * Veredicto no transitorio del storage: la subida es inconsistente o falló de forma
 * definitiva; el orquestador ejecuta rollback y, según el caso, penalidad.
 */
class UploadVerdictException extends RuntimeException {

  private final String code;

  UploadVerdictException(String code, String message) {
    super(message);
    this.code = code;
  }

  String getCode() {
    return this.code;
  }
}
