package com.guille.media.bff.app.ports;

import com.guille.media.bff.app.dto.UserProfile;

import reactor.core.publisher.Mono;

/** Contrato hacia mvflix-users (perfil del usuario autenticado). */
public interface UsersWebPort {

  /**
   * @return perfil del usuario, o vacío si el bearer no es válido (401).
   */
  Mono<UserProfile> me(String bearer);
}