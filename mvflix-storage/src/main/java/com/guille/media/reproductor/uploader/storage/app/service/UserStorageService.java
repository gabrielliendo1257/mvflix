package com.guille.media.reproductor.uploader.storage.app.service;

import com.guille.media.reproductor.uploader.storage.domain.models.UserStorage;

import reactor.core.publisher.Mono;

/** Gestión del espacio del usuario: consulta de cuota y provisión del bucket dedicado. */
public interface UserStorageService {
  Mono<UserStorage> getUserStorage();

  /** Consulta el uso real de un usuario concreto (contrato M2M con user-service). */
  Mono<UserStorage> getUserStorageBy(String username);

  /**
   * Asegura que el usuario tiene su espacio en el bucket dedicado:
   * crea la fila de {@code user_storage} si no existe y materializa las
   * subcarpetas en el objeto storage. Idempotente. Se invoca al registrar
   * o loguear el usuario.
   */
  Mono<Void> ensureUserStorage(String username, long quotaBytes);
}