package com.guille.media.bff.app.service;

import org.springframework.http.HttpStatus;

/**
 * Error del flujo orquestado de subida: el BFF ya ejecutó su veredicto (rollback, penalidad)
 * y entrega al front un código de diagnóstico en lugar de un 500 genérico.
 */
public class UploadOrchestrationException extends RuntimeException {

  private final HttpStatus status;
  private final String code;

  public UploadOrchestrationException(HttpStatus status, String code, String message) {
    super(message);
    this.status = status;
    this.code = code;
  }

  public HttpStatus getStatus() {
    return this.status;
  }

  public String getCode() {
    return this.code;
  }
}
