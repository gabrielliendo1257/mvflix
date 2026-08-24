package com.guille.media.bff.experience.addmedia.application;

/** La intención del alta está incompleta: falta información del candidato. */
public class InvalidIntentException extends RuntimeException {

  public InvalidIntentException(String message) {
    super(message);
  }
}
