package com.guille.media.reproductor.uploader;

import static org.assertj.core.api.Assertions.assertThat;

import com.guille.media.reproductor.uploader.storage.app.service.TerminalUploadTransition;
import com.guille.media.reproductor.uploader.storage.domain.exceptions.IllegalStateTransitionException;
import com.guille.media.reproductor.uploader.storage.domain.vos.StorageKey;
import com.guille.media.reproductor.uploader.storage.domain.models.StoreObject;
import com.guille.media.reproductor.uploader.storage.domain.models.StoreObject.StorageSessionStatus;
import com.guille.media.reproductor.uploader.storage.domain.models.UserStorage;
import com.guille.media.reproductor.uploader.storage.domain.ports.StorageRepository;
import com.guille.media.reproductor.uploader.storage.domain.ports.UserStorageRepository;
import com.guille.media.reproductor.uploader.storage.domain.vos.BucketName;
import com.guille.media.reproductor.uploader.storage.domain.vos.StorageMetadata;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.reactive.TransactionalOperator;
import org.testcontainers.containers.MinIOContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Instant;

/**
 * Pruebas de contabilidad contra PostgreSQL REAL (no mocks): demuestran que la
 * reserva de cuota + guardado de sesión comparten transacción (el fallo del
 * save revierte el consumo) y que el CAS de TerminalUploadTransition solo
 * libera cuota para el ganador de la transición.
 */
@Testcontainers
@ActiveProfiles("sandbox")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class UploadAccountingIntegrationTest {

  private static final String USERNAME = "accounting-user";
  private static final long QUOTA_BYTES = 10_000L;
  private static final long RESERVE_BYTES = 5_000L;

  @Container
  static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

  @Container
  static final MinIOContainer MINIO =
      new MinIOContainer("minio/minio:latest")
          .withUserName("minioadmin")
          .withPassword("minioadmin");

  @DynamicPropertySource
  static void properties(DynamicPropertyRegistry registry) {
    registry.add(
        "spring.r2dbc.url",
        () ->
            "r2dbc:postgresql://"
                + POSTGRES.getHost()
                + ":"
                + POSTGRES.getMappedPort(5432)
                + "/"
                + POSTGRES.getDatabaseName());
    registry.add("spring.r2dbc.username", POSTGRES::getUsername);
    registry.add("spring.r2dbc.password", POSTGRES::getPassword);
    registry.add(
        "spring.datasource.url",
        () ->
            "jdbc:postgresql://"
                + POSTGRES.getHost()
                + ":"
                + POSTGRES.getMappedPort(5432)
                + "/"
                + POSTGRES.getDatabaseName());
    registry.add("spring.datasource.username", POSTGRES::getUsername);
    registry.add("spring.datasource.password", POSTGRES::getPassword);
    registry.add("minio.url", MINIO::getS3URL);
    registry.add("minio.access-key", MINIO::getUserName);
    registry.add("minio.secret-key", MINIO::getPassword);
  }

  @Autowired private DatabaseClient databaseClient;
  @Autowired private UserStorageRepository userStorageRepository;
  @Autowired private StorageRepository storageRepository;
  @Autowired private TransactionalOperator transactionalOperator;
  @Autowired private TerminalUploadTransition terminalTransition;

  private void provisionUser(String username) {
    this.databaseClient
        .sql(
            """
			INSERT INTO user_storage (owner_username, bucket_name, storage_quota, storage_usage, created_at)
			VALUES (:u, 'movies', :q, 0, NOW())
			ON CONFLICT (owner_username) DO NOTHING
			""")
        .bind("u", username)
        .bind("q", QUOTA_BYTES)
        .fetch()
        .rowsUpdated()
        .block();
  }

  private long usageOf(String username) {
    return this.userStorageRepository
        .findByOwnerUsername(username)
        .map(UserStorage::getStorageUsage)
        .map(u -> u.getCurrentBytesUsage())
        .block();
  }

  private StoreObject pendingObject(Long id, String objectKey, long size) {
    return new StoreObject(
        USERNAME,
        new StorageKey(objectKey),
        new StorageMetadata("video/mp4", size, null, Instant.now()),
        Instant.now(),
        id,
        StorageSessionStatus.PENDING);
  }

  @Test
  void reservationRollsBackWhenSessionSaveFails() {
    this.provisionUser(USERNAME);

    // Fila preexistente con la misma object_key: fuerza una violación UNIQUE en
    // el INSERT del save, DESPUÉS de haber reservado cuota dentro de la tx.
    StoreObject existing = this.pendingObject(null, "dup/videos/conflict.mp4", RESERVE_BYTES);
    this.storageRepository.save(existing).block();

    StoreObject conflicting = this.pendingObject(null, "dup/videos/conflict.mp4", RESERVE_BYTES);

    StepVerifier.create(
            this.transactionalOperator.transactional(
                this.userStorageRepository
                    .consumeStorage(USERNAME, RESERVE_BYTES)
                    .filter(rows -> rows == 1)
                    .switchIfEmpty(
                        Mono.error(new IllegalStateException("conditional reserve rejected")))
                    .then(this.storageRepository.save(conflicting))))
        .expectError(DataIntegrityViolationException.class)
        .verify();

    // La reserva se revirtió con la tx: sin bytes huérfanos ni sesión extra.
    assertThat(this.usageOf(USERNAME))
        .as("usage must equal pre-transaction value")
        .isZero();
  }

  @Test
  void reconcileTransitionWithRealCasReleasesQuotaExactlyOnce() {
    this.provisionUser(USERNAME);

    StoreObject pending = this.pendingObject(null, "cas/videos/a.mp4", RESERVE_BYTES);
    StoreObject persisted = this.storageRepository.save(pending).block();
    // Simulamos la reserva hecha al crear la sesión (consumeStorage).
    this.databaseClient
        .sql("UPDATE user_storage SET storage_usage = :u WHERE owner_username = :o")
        .bind("u", RESERVE_BYTES)
        .bind("o", USERNAME)
        .fetch()
        .rowsUpdated()
        .block();

    // Reproducimos el flujo productivo: mutación en memoria y CAS sobre la
    // fila aún PENDING.
    persisted.markFailed();

    // Primer intento: CAS PENDING->FAILED gana y libera exactamente una vez.
    StepVerifier.create(
            this.terminalTransition.transitionAndRelease(persisted, StorageSessionStatus.PENDING))
        .expectNextCount(1)
        .verifyComplete();

    assertThat(this.usageOf(USERNAME)).isZero();

    StoreObject reread = this.storageRepository.findById(persisted.getStorageId()).block();
    assertThat(reread.getStorageObjectStatus()).isEqualTo(StorageSessionStatus.FAILED);

    // Segundo intento sobre la misma fila: el CAS pierde y NO vuelve a liberar.
    StepVerifier.create(
            this.terminalTransition.transitionAndRelease(persisted, StorageSessionStatus.PENDING))
        .expectError(IllegalStateTransitionException.class)
        .verify();

    assertThat(this.usageOf(USERNAME)).isZero();
  }
}
