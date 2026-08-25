package com.guille.media.bff.experience.playback.application.port;

import java.time.Instant;

import reactor.core.publisher.Mono;

/**
 * Capability temporal para entregar contenido LOCAL por el proxy del BFF.
 *
 * <p>Mientras el navegador no alcance al servicio de storage (topología LAN),
 * los bytes viajan BFF->navegador. El token HMAC liga (media, asset,
 * biblioteca, ruta) con expiración corta y SIN credenciales del usuario
 * dentro: a la hora de servir, el BFF resuelve el access token vivo desde la
 * sesión OAuth2 del sujeto, nunca desde el query param.
 */
public interface LocalPlaybackAccess {

  /** Emite la capability firmada para un asset de biblioteca. */
  Mono<MintedAccess> mint(LocalMintCommand command);

  /** Verifica firma/expiración y devuelve a qué asset da acceso. */
  Mono<LocalGrant> resolve(String rawToken);

  record LocalMintCommand(
      long mediaId, long assetId, long libraryId, String relativePath, String subject) {}

  record MintedAccess(String rawToken, Instant expiresAt) {}

  record LocalGrant(
      long mediaId, long assetId, long libraryId, String relativePath, String subject,
      Instant expiresAt) {}
}
