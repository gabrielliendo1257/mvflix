package com.guille.media.bff.experience.addmedia.application;

/** Storage respondió algo que viola el contrato del alta (p.ej. uploadId). */
public class InvalidStorageResponseException extends RuntimeException {

  public InvalidStorageResponseException(String detail) {
    super(detail);
  }
}
