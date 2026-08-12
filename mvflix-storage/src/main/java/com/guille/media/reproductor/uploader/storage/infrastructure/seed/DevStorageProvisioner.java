package com.guille.media.reproductor.uploader.storage.infrastructure.seed;

import com.guille.mvflix.devseed.DevUser;
import com.guille.mvflix.devseed.DevUserSeeder;
import com.guille.media.reproductor.uploader.storage.domain.service.UserStorageService;

import lombok.extern.slf4j.Slf4j;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * Seed de entorno dev: provisiona el espacio de almacenamiento de los usuarios
 * definidos en {@code dev-users.yaml}, reutilizando el flujo de dominio
 * {@link UserStorageService#ensureUserStorage}.
 *
 * <p>Idempotente: si la fila ya existe, solo se garantiza el layout de carpetas
 * en el bucket.
 */
@Slf4j
@Component
@Profile("dev")
public class DevStorageProvisioner implements DevUserSeeder {

  private static final Duration TIMEOUT = Duration.ofSeconds(30);

  private final UserStorageService userStorageService;

  public DevStorageProvisioner(UserStorageService userStorageService) {
    this.userStorageService = userStorageService;
  }

  @Override
  public void seed(DevUser devUser) {
    try {
      this.userStorageService
          .ensureUserStorage(devUser.getUsername(), devUser.getQuotaBytes())
          .block(TIMEOUT);
      log.info("Dev storage provisioned: username={}", devUser.getUsername());
    } catch (Exception error) {
      log.warn(
          "Dev storage could not be provisioned (puede provisionarse a mano via POST /provision):"
              + " username={}, cause={}",
          devUser.getUsername(),
          error.getMessage());
    }
  }
}