package com.guille.media.bff.app.ports;

import com.guille.media.bff.app.dto.UserProfile;

import reactor.core.publisher.Mono;

/** Contrato hacia mvflix-users (perfil del usuario autenticado). */
public interface UsersWebPort {

  /** Perfil del usuario de la sesión OAuth2 del navegador. */
  Mono<UserProfile> me();
}