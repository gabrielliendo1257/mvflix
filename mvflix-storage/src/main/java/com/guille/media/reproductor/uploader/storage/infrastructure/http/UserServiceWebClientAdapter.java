package com.guille.media.reproductor.uploader.storage.infrastructure.http;

import com.guille.media.reproductor.uploader.storage.app.user.UserServiceCommandPort;

import lombok.RequiredArgsConstructor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Adaptador reactivo del puerto {@link UserServiceCommandPort}.
 *
 * <p>Usa WebClient (I/O no bloqueante, compatible con WebFlux) en lugar de un
 * cliente Feign bloqueante. El token lo adjunta el filtro
 * {@code ServerOAuth2AuthorizedClientExchangeFilterFunction}.
 */
@Service
@Profile("!sandbox")
@RequiredArgsConstructor
public class UserServiceWebClientAdapter implements UserServiceCommandPort {

  private static final Logger log = LoggerFactory.getLogger(UserServiceWebClientAdapter.class);

  private final WebClient userServiceWebClient;

  @Override
  public void applyQuota(String subject, Long quota) {
    this.userServiceWebClient.post()
        .uri("/api/v1/users/quota?subject={subject}&quota={quota}", subject, quota)
        .retrieve()
        .toBodilessEntity()
        .subscribe(
            response -> log.debug("Quota notificada al user-service: subject={} quota={}",
                    subject, quota),
            error -> log.error("Fallo al notificar quota al user-service: subject={} quota={}",
                    subject, quota, error));
  }
}