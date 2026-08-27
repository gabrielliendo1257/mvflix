package com.guille.media.reproductor.uploader.storage.managedstorage.application.command.request;

/**
 * Comando para eliminar un objeto almacenado. El borrado es idempotente y
 * retry-safe; el use case dueño de la transición es
 * {@code DeleteStoredObject}. No incluye ownership: el llamante (endpoint de
 * usuario o M2M Movies→Storage) ya autorizó antes de delegar.
 */
public record DeleteStoredObjectCommand(Long storageId) {}
