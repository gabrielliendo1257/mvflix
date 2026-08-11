package com.guille.media.reproductor.uploader.storage.infrastructure.seed;

import com.guille.media.reproductor.uploader.storage.domain.service.UserStorageService;

import lombok.extern.slf4j.Slf4j;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;

/**
 * Seed de entorno dev: provisiona el espacio de almacenamiento de los mismos usuarios de prueba
 * que siembra el authorization-service (Javier, Admin), reutilizando el flujo de dominio
 * {@link UserStorageService#ensureUserStorage}.
 *
 * <p>Espejo de {@code seedDevData} del IdP: cada servicio siembra su propio dato dev en su propio
 * arranque; el compose se mantiene como infraestructura pura. Idempotente: si la fila ya existe,
 * solo se garantiza el layout de carpetas en el bucket.
 */
@Slf4j
@Component
@Profile("dev")
public class DevStorageProvisioner implements ApplicationRunner {

  private static final long GIGABYTE = 1024L * 1024 * 1024;
  private static final Duration TIMEOUT = Duration.ofSeconds(30);

  private static final List<DevUser> DEV_USERS =
      List.of(new DevUser("Javier", 10 * GIGABYTE), new DevUser("Admin", 10 * GIGABYTE));

  private final UserStorageService userStorageService;

  public DevStorageProvisioner(UserStorageService userStorageService) {
    this.userStorageService = userStorageService;
  }

  @Override
  public void run(ApplicationArguments args) {
    DEV_USERS.forEach(this::provision);
  }

  private void provision(DevUser devUser) {
    try {
      this.userStorageService
          .ensureUserStorage(devUser.username(), devUser.quotaBytes())
          .block(TIMEOUT);
      log.info("Dev storage provisioned: username={}", devUser.username());
    } catch (Exception error) {
      log.warn(
          "Dev storage could not be provisioned (puede provisionarse a mano via POST /provision):"
              + " username={}, cause={}",
          devUser.username(),
          error.getMessage());
    }
  }

  private record DevUser(String username, long quotaBytes) {}
}