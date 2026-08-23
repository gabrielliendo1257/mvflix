package com.guille.media.bff.shared.error;

/** Error de aplicación: el recurso solicitado no existe. Se traduce a 404. */
public class EntityNotFound extends RuntimeException {

  public EntityNotFound(String message) {
    super(message);
  }
}
