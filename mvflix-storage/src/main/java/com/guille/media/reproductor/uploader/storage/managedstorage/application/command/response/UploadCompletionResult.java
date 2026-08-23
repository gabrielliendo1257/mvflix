package com.guille.media.reproductor.uploader.storage.managedstorage.application.command.response;

/**
 * Resultado de la confirmación de un upload.
 *
 * <p>Distinguisce el caso en que el object store aún no tiene el objeto
 * (la confirmación del cliente llegó antes que los bytes): la sesión queda
 * {@code PENDING} y el webhook de MinIO la reconciliará; el cliente recibe 202.
 */
public record UploadCompletionResult(UploadCompletionStatus status) {

  public enum UploadCompletionStatus {
    /** El objeto quedó COMPLETED y el evento de dominio ya se publicó. */
    COMPLETED,
    /** La confirmación llegó antes que el objeto: sigue PENDING, sin acciones destructivas. */
    PENDING_VERIFICATION
  }

  public static UploadCompletionResult completed() {
    return new UploadCompletionResult(UploadCompletionStatus.COMPLETED);
  }

  public static UploadCompletionResult pendingVerification() {
    return new UploadCompletionResult(UploadCompletionStatus.PENDING_VERIFICATION);
  }
}
