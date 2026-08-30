package com.guille.media.bff.infrastructure.persistence;

import com.guille.media.bff.experience.addmedia.application.IdempotencyConflictException;
import com.guille.media.bff.experience.addmedia.application.port.AddMediaProcessRepository;
import com.guille.media.bff.experience.addmedia.model.AddMediaId;
import com.guille.media.bff.experience.addmedia.model.AddMediaPhase;
import com.guille.media.bff.experience.addmedia.model.AddMediaProcess;
import com.guille.media.bff.experience.addmedia.model.ConcurrentProcessUpdateException;
import java.time.Duration;
import java.util.Set;
import io.r2dbc.spi.Row;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Repository;
import org.springframework.context.annotation.Profile;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/** Durable process store. Transitions are SQL compare-and-set operations. */
@Repository
@Profile("!local")
public class R2dbcAddMediaProcessRepository implements AddMediaProcessRepository {
  private static final String COLUMNS = "id, owner_subject, movie_id, upload_id, phase, failure_code, version";
  private final DatabaseClient database;

  public R2dbcAddMediaProcessRepository(DatabaseClient database) {
    this.database = database;
  }

  @Override
  public Mono<AddMediaProcess> createIfAbsent(String owner, String key, String fingerprint) {
    return database.sql("INSERT INTO add_media_processes (id, owner_subject, idempotency_key, fingerprint, phase, version) "
        + "VALUES (:id, :owner, :key, :fingerprint, 'STARTING', 0) "
        + "ON CONFLICT (owner_subject, idempotency_key) DO NOTHING")
        .bind("id", AddMediaId.newId().value()).bind("owner", owner).bind("key", key)
        .bind("fingerprint", fingerprint).fetch().rowsUpdated()
        .flatMap(ignored -> database.sql("SELECT " + COLUMNS + ", fingerprint FROM add_media_processes "
            + "WHERE owner_subject = :owner AND idempotency_key = :key")
            .bind("owner", owner).bind("key", key).map((row, metadata) -> {
              if (!fingerprint.equals(row.get("fingerprint", String.class))) {
                throw new IdempotencyConflictException(key);
              }
              return map(row);
            }).one());
  }

  @Override
  public Mono<AddMediaProcess> findById(AddMediaId id) {
    return database.sql("SELECT " + COLUMNS + " FROM add_media_processes WHERE id = :id")
        .bind("id", id.value()).map((row, metadata) -> map(row)).one();
  }

  @Override public Mono<Boolean> tryClaim(AddMediaId id) {
    return claim(id, Set.of("STARTING"), "PREPARING");
  }

  @Override public Mono<Boolean> tryFinalizeClaim(AddMediaId id) {
    return claim(id, Set.of("WAITING_FOR_UPLOAD", "VERIFYING_UPLOAD"), "FINALIZING");
  }

  @Override public Mono<Boolean> tryCancelClaim(AddMediaId id) {
    return claim(id, Set.of("WAITING_FOR_UPLOAD", "VERIFYING_UPLOAD"), "CANCELLING");
  }

  @Override
  public Mono<Boolean> tryCompleteCancellation(AddMediaId id) {
    return database.sql("UPDATE add_media_processes SET phase = 'CANCELLED', version = version + 1, "
        + "updated_at = CURRENT_TIMESTAMP WHERE id = :id AND phase = 'CANCELLING' "
        + "AND NOT EXISTS (SELECT 1 FROM add_media_compensations "
        + "WHERE process_id = :id AND status = 'PENDING')")
        .bind("id", id.value()).fetch().rowsUpdated().map(rows -> rows == 1);
  }

  @Override
  public Mono<Boolean> completePreparingRecovery(AddMediaId id) {
    return database.sql("UPDATE add_media_processes SET phase = CASE "
         + "WHEN movie_id IS NULL AND upload_id IS NULL THEN 'STARTING' "
         + "WHEN movie_id IS NOT NULL AND upload_id IS NULL THEN 'STARTING' "
        + "ELSE phase END, "
        + "version = version + 1, updated_at = CURRENT_TIMESTAMP WHERE id = :id "
         + "AND phase = 'PREPARING' AND (movie_id IS NULL AND upload_id IS NULL OR "
         + "movie_id IS NOT NULL AND upload_id IS NULL) AND NOT EXISTS (SELECT 1 FROM add_media_compensations "
         + "WHERE process_id = :id AND status = 'PENDING')")
        .bind("id", id.value()).fetch().rowsUpdated().map(rows -> rows == 1);
  }

  @Override
  public Mono<Boolean> completePreparingRecovery(AddMediaId id, boolean uploadConfirmedAbsent) {
    if (!uploadConfirmedAbsent) return Mono.just(false);
    return database.sql("UPDATE add_media_processes SET phase = 'CANCELLED', version = version + 1, "
        + "updated_at = CURRENT_TIMESTAMP WHERE id = :id AND phase = 'PREPARING' "
        + "AND movie_id IS NOT NULL AND upload_id IS NULL AND NOT EXISTS (SELECT 1 FROM "
        + "add_media_compensations WHERE process_id = :id AND status = 'PENDING')")
        .bind("id", id.value()).fetch().rowsUpdated().map(rows -> rows == 1);
  }

  @Override
  public Mono<Boolean> claimRecoveredCancellation(AddMediaId id, long version, Long uploadId) {
    return database.sql("UPDATE add_media_processes SET upload_id = :upload, phase = 'CANCELLING', "
        + "version = version + 1, updated_at = CURRENT_TIMESTAMP WHERE id = :id "
        + "AND version = :version AND phase = 'PREPARING' AND movie_id IS NOT NULL AND upload_id IS NULL")
        .bind("id", id.value()).bind("version", version).bind("upload", uploadId)
        .fetch().rowsUpdated().map(rows -> rows == 1);
  }

  private Mono<Boolean> claim(AddMediaId id, Set<String> phases, String next) {
    return database.sql("UPDATE add_media_processes SET phase = :next, version = version + 1, "
        + "updated_at = CURRENT_TIMESTAMP WHERE id = :id AND phase IN (:phases)")
        .bind("next", next).bind("id", id.value()).bind("phases", phases)
        .fetch().rowsUpdated().map(rows -> rows == 1);
  }

  @Override
  public Mono<AddMediaProcess> releaseClaim(AddMediaId id) {
    return database.sql("UPDATE add_media_processes SET movie_id = NULL, upload_id = NULL, phase = 'STARTING', version = version + 1, "
        + "updated_at = CURRENT_TIMESTAMP WHERE id = :id AND phase = 'PREPARING'")
        .bind("id", id.value()).fetch().rowsUpdated()
        .flatMap(rows -> rows == 1 ? findById(id) : Mono.empty());
  }

  @Override
  public Mono<AddMediaProcess> save(AddMediaProcess process) {
    DatabaseClient.GenericExecuteSpec statement = database.sql("UPDATE add_media_processes SET movie_id = :movie, "
        + "upload_id = :upload, phase = :phase, failure_code = :failure, version = version + 1, "
        + "updated_at = CURRENT_TIMESTAMP WHERE id = :id AND version = :version "
        + "AND phase IN (:expectedPhases)")
        .bind("id", process.id().value()).bind("phase", process.phase().name())
        .bind("version", process.version() - 1).bind("expectedPhases", expectedPhases(process));
    statement = nullable(statement, "movie", process.movieId(), Long.class);
    statement = nullable(statement, "upload", process.uploadId(), Long.class);
    statement = nullable(statement, "failure", process.failureCode(), String.class);
    return statement.fetch().rowsUpdated().flatMap(rows -> rows == 1 ? findById(process.id())
        : Mono.error(new ConcurrentProcessUpdateException(process.id())));
  }

  private static Set<String> expectedPhases(AddMediaProcess process) {
    return switch (process.phase()) {
      case STARTING -> Set.of("PREPARING");
      case PREPARING -> Set.of("PREPARING");
      case WAITING_FOR_UPLOAD -> Set.of("PREPARING");
      case VERIFYING_UPLOAD -> Set.of("WAITING_FOR_UPLOAD", "FINALIZING");
      case READY, FAILED -> Set.of("FINALIZING");
      case CANCELLED -> Set.of("CANCELLING");
      default -> throw new IllegalArgumentException("Fase no persistible con save: " + process.phase());
    };
  }

  /** Observability-only query. The original command is not in the current domain model. */
  public Flux<StaleProcess> findStaleClaims(Duration age) {
     return database.sql("SELECT id, owner_subject, idempotency_key, phase, movie_id, upload_id, version "
         + "FROM add_media_processes WHERE phase IN "
        + "('PREPARING','CANCELLING','FINALIZING') AND updated_at < CURRENT_TIMESTAMP "
        + "- (:age * INTERVAL '1 millisecond')").bind("age", age.toMillis())
         .map((row, metadata) -> new StaleProcess(AddMediaId.parse(row.get("id", String.class)),
             row.get("owner_subject", String.class), row.get("idempotency_key", String.class),
             AddMediaPhase.valueOf(row.get("phase", String.class)), row.get("movie_id", Long.class),
             row.get("upload_id", Long.class), row.get("version", Long.class))).all();
  }

  private static DatabaseClient.GenericExecuteSpec nullable(DatabaseClient.GenericExecuteSpec statement,
      String name, Object value, Class<?> type) {
    return value == null ? statement.bindNull(name, type) : statement.bind(name, value);
  }

  private static AddMediaProcess map(Row row) {
    return new AddMediaProcess(AddMediaId.parse(row.get("id", String.class)),
        row.get("owner_subject", String.class), row.get("movie_id", Long.class),
        row.get("upload_id", Long.class), AddMediaPhase.valueOf(row.get("phase", String.class)),
        row.get("failure_code", String.class), row.get("version", Long.class));
  }

  public record StaleProcess(AddMediaId id, String ownerSubject, String idempotencyKey,
      AddMediaPhase phase, Long movieId, Long uploadId, long version) {}
}
