package com.guille.media.bff.experience.media.application;

/**
 * Borrado de media MANAGED bloqueado: el catálogo se borraría pero el objeto
 * de storage (y su cuota) quedarían huérfanos. Hasta que exista una
 * compensación durable (outbox/evento o estado DELETING con reintentos), no
 * se permite. HTTP 409.
 */
public class ManagedDeleteBlockedException extends RuntimeException {

  public ManagedDeleteBlockedException(long mediaId, String source) {
    super("Cannot delete " + source + " media " + mediaId
        + ": storage cleanup is not yet durable");
  }
}
