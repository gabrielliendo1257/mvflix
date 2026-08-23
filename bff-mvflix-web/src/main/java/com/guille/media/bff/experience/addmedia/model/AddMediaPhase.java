package com.guille.media.bff.experience.addmedia.model;

/**
 * Fase visible para la UX del proceso Add Media. Es estado de EXPERIENCIA del
 * BFF: no duplica el estado autoritativo del upload (storage) ni el estado
 * editorial de la película (movies).
 */
public enum AddMediaPhase {
  STARTING,
  WAITING_FOR_UPLOAD,
  VERIFYING_UPLOAD,
  FINALIZING,
  READY,
  FAILED,
  CANCELLED
}
