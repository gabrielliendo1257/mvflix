package com.guille.media.reproductor.uploader.storage.managedstorage.infrastructure.persistence;

import com.guille.media.reproductor.uploader.storage.managedstorage.domain.port.OrphanCleanupQueue;

import lombok.RequiredArgsConstructor;

import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Component;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
public class SpringDataOrphanCleanupQueue implements OrphanCleanupQueue {

  private final DatabaseClient databaseClient;

  @Override
  public Mono<Void> enqueue(String bucket, String objectKey, String ownerUsername,
      String reason) {
    return this.databaseClient
        .sql(
            """
            INSERT INTO orphan_cleanup_tasks (bucket, object_key, owner_username, reason)
            VALUES (:bucket, :key, :owner, :reason)
            ON CONFLICT (bucket, object_key, processed_at)
            DO UPDATE SET attempts = orphan_cleanup_tasks.attempts + 1,
                          last_error = EXCLUDED.reason
            """)
        .bind("bucket", bucket)
        .bind("key", objectKey)
        .bind("owner", ownerUsername)
        .bind("reason", reason)
        .then();
  }

  @Override
  public Flux<OrphanTask> pending(int limit) {
    return this.databaseClient
        .sql(
            """
            SELECT id, bucket, object_key, COALESCE(owner_username,'') AS owner_username,
                   reason, attempts
            FROM orphan_cleanup_tasks
            WHERE processed_at IS NULL
            ORDER BY created_at
            LIMIT :limit
            """)
        .bind("limit", limit)
        .map((row, meta) -> new OrphanTask(
            row.get("id", Long.class),
            row.get("bucket", String.class),
            row.get("object_key", String.class),
            row.get("owner_username", String.class),
            row.get("reason", String.class),
            row.get("attempts", Integer.class)))
        .all();
  }

  @Override
  public Mono<Void> markProcessed(long taskId) {
    return this.databaseClient
        .sql("UPDATE orphan_cleanup_tasks SET processed_at = NOW() WHERE id = :id")
        .bind("id", taskId)
        .then();
  }
}
