package com.gcorp.service.app.mvflix_media_ingestion.infrastructure.persistence;

import com.gcorp.service.app.mvflix_media_ingestion.application.CompensationRepository;
import java.util.UUID;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.*;

@Repository
public class DatabaseCompensationRepository implements CompensationRepository {
  private final DatabaseClient db;

  public DatabaseCompensationRepository(DatabaseClient db) {
    this.db = db;
  }

  public Mono<Void> schedule(UUID ingestionId, String action) {
    return db.sql(
            "INSERT INTO media_ingestion_compensations(compensation_id,ingestion_id,action) VALUES(:id,:ing,:a) ON CONFLICT(ingestion_id,action) DO NOTHING")
        .bind("id", UUID.randomUUID())
        .bind("ing", ingestionId)
        .bind("a", action)
        .fetch()
        .rowsUpdated()
        .then();
  }

  public Flux<Compensation> due(int limit) {
    return db.sql(
            "UPDATE media_ingestion_compensations SET claimed_until=now()+interval '1 minute',attempts=attempts+1 WHERE compensation_id IN (SELECT compensation_id FROM media_ingestion_compensations WHERE status='PENDING' AND next_attempt_at<=now() AND (claimed_until IS NULL OR claimed_until<now()) ORDER BY next_attempt_at FOR UPDATE SKIP LOCKED LIMIT :n) RETURNING compensation_id,ingestion_id,action")
        .bind("n", limit)
        .map(
            (r, m) ->
                new Compensation(
                    r.get("compensation_id", UUID.class),
                    r.get("ingestion_id", UUID.class),
                    r.get("action", String.class)))
        .all();
  }

  public Mono<Void> success(UUID id) {
    return db.sql(
            "UPDATE media_ingestion_compensations SET status='COMPLETED',claimed_until=NULL WHERE compensation_id=:id")
        .bind("id", id)
        .fetch()
        .rowsUpdated()
        .then();
  }

  public Mono<Void> failure(UUID id, String error) {
    return db.sql(
            "UPDATE media_ingestion_compensations SET status='PENDING',claimed_until=NULL,next_attempt_at=now()+interval '1 minute',last_error=:e WHERE compensation_id=:id")
        .bind("id", id)
        .bind("e", error)
        .fetch()
        .rowsUpdated()
        .then();
  }
}
