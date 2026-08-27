package com.guille.media.reproductor.uploader.storage.managedstorage.infrastructure.web;

/**
 * Petición M2M de borrado de un objeto MANAGED. El llamante declara el owner y
 * el objectKey que ESPERA: si no coinciden con el objeto real, storage rechaza
 * (asociación corrupta) en lugar de borrar el objeto equivocado.
 */
public record ManagedObjectDeletionRequest(String expectedOwner, String expectedObjectKey) {}
