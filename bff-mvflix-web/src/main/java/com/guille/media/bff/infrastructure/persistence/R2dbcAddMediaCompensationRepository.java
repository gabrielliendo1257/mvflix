package com.guille.media.bff.infrastructure.persistence;

import com.guille.media.bff.experience.addmedia.application.port.AddMediaCompensationRepository;
import com.guille.media.bff.experience.addmedia.model.AddMediaId;
import io.r2dbc.spi.Row;
import org.springframework.context.annotation.Profile;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
@Profile("!local")
public class R2dbcAddMediaCompensationRepository implements AddMediaCompensationRepository {
  private final DatabaseClient database;

  public R2dbcAddMediaCompensationRepository(DatabaseClient database) {
    this.database = database;
  }

  @Override
  public Mono<Void> enqueue(AddMediaId processId, Kind kind, Long resourceId, Throwable error) {
    String message = error == null ? null : error.getClass().getSimpleName() + ": " + error.getMessage();
    DatabaseClient.GenericExecuteSpec query = database.sql(
        "INSERT INTO add_media_compensations (process_id, kind, resource_id, last_error) "
            + "VALUES (:process, :kind, :resource, :error) "
            + "ON CONFLICT (process_id, kind, resource_id) DO UPDATE SET updated_at = CURRENT_TIMESTAMP")
        .bind("process", processId.value()).bind("kind", kind.name()).bind("resource", resourceId);
    query = message == null ? query.bindNull("error", String.class) : query.bind("error", message);
    return query.fetch().rowsUpdated().then();
  }

  @Override
  public Flux<Task> claimPending(int limit) {
    return database.sql(
        "WITH picked AS (SELECT id FROM add_media_compensations "
            + "WHERE status = 'PENDING' AND next_attempt_at <= CURRENT_TIMESTAMP "
            + "ORDER BY next_attempt_at, id FOR UPDATE SKIP LOCKED LIMIT :limit) "
            + "UPDATE add_media_compensations t SET attempts = t.attempts + 1, "
            + "next_attempt_at = CURRENT_TIMESTAMP + INTERVAL '10 minutes', "
            + "updated_at = CURRENT_TIMESTAMP FROM picked WHERE t.id = picked.id "
            + "RETURNING t.id, t.process_id, t.kind, t.resource_id, t.attempts, t.last_error")
        .bind("limit", limit).map((row, metadata) -> map(row)).all();
  }

  @Override
  public Mono<Void> markCompleted(long taskId) {
    return database.sql("UPDATE add_media_compensations SET status = 'COMPLETED', "
        + "last_error = NULL, updated_at = CURRENT_TIMESTAMP WHERE id = :id")
        .bind("id", taskId).fetch().rowsUpdated().then();
  }

  @Override
  public Mono<Void> markFailed(long taskId, int attempts, Throwable error) {
    String message = error.getClass().getSimpleName() + ": " + error.getMessage();
    return database.sql("UPDATE add_media_compensations SET status = 'PENDING', last_error = :error, "
        + "next_attempt_at = CURRENT_TIMESTAMP + (LEAST(:attempts, 10) * 2 + 1) * INTERVAL '1 second', "
        + "updated_at = CURRENT_TIMESTAMP WHERE id = :id")
        .bind("id", taskId).bind("attempts", attempts).bind("error", message)
        .fetch().rowsUpdated().then();
  }

  private static Task map(Row row) {
    return new Task(row.get("id", Long.class), AddMediaId.parse(row.get("process_id", String.class)),
        Kind.valueOf(row.get("kind", String.class)), row.get("resource_id", Long.class),
        row.get("attempts", Integer.class), row.get("last_error", String.class));
  }
}
