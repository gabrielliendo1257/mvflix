package com.guille.media.bff.experience.addmedia.application;

/** Misma idempotencyKey reutilizada con un payload distinto. */
public class IdempotencyConflictException extends RuntimeException {

  private final String code;

  public IdempotencyConflictException(String idempotencyKey) {
    super("idempotencyKey ya usada con un payload distinto: " + idempotencyKey);
    this.code = "IDEMPOTENCY_CONFLICT";
  }

  public String getCode() {
    return this.code;
  }
}
