package com.guille.media.bff.app.service;

/** Estado transitorio (PENDING): se reintenta antes de declarar fallo. */
class PendingUploadException extends RuntimeException {

  PendingUploadException() {
    super("Upload session still pending");
  }
}
