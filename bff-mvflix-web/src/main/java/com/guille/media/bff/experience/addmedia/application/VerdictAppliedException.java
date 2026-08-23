package com.guille.media.bff.experience.addmedia.application;

/**
 * Veredicto DEFINITIVO aplicado: el rollback (película + objeto) ya corrió y,
 * según el caso, se registró penalidad. El proceso Add Media debe pasar a
 * FAILED y el front recibe 409 con este código.
 */
public class VerdictAppliedException extends RuntimeException {

  private final String code;

  public VerdictAppliedException(String code, String message) {
    super(message);
    this.code = code;
  }

  public String getCode() {
    return this.code;
  }
}
