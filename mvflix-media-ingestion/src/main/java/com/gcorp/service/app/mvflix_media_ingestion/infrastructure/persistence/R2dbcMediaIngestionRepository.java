package com.gcorp.service.app.mvflix_media_ingestion.infrastructure.persistence;

import com.gcorp.service.app.mvflix_media_ingestion.application.MediaIngestionRepository;
import com.gcorp.service.app.mvflix_media_ingestion.domain.MediaIngestion;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;
import java.time.Instant;
import java.util.UUID;

@Repository
public class R2dbcMediaIngestionRepository implements MediaIngestionRepository {
  private final DatabaseClient db;
  public R2dbcMediaIngestionRepository(DatabaseClient db) { this.db=db; }
  private MediaIngestion map(io.r2dbc.spi.Row r) { return new MediaIngestion(r.get("ingestion_id",UUID.class),r.get("actor_id",String.class),r.get("catalog_item_id",Long.class),r.get("upload_id",String.class),MediaIngestion.Phase.valueOf(r.get("phase",String.class)),r.get("failure_code",String.class),r.get("version",Long.class),r.get("retry_count",Integer.class),r.get("created_at",Instant.class),r.get("updated_at",Instant.class),r.get("next_attempt_at",Instant.class),r.get("idempotency_key",String.class),r.get("file_name",String.class),r.get("file_size",Long.class),r.get("mime_type",String.class),r.get("upload_url",String.class),r.get("storage_id",Long.class)); }
  private org.springframework.r2dbc.core.DatabaseClient.GenericExecuteSpec bind(org.springframework.r2dbc.core.DatabaseClient.GenericExecuteSpec s,String n,Object v,Class<?> type) { return v == null ? s.bindNull(n,type) : s.bind(n,v); }
  private String select() { return "SELECT * FROM media_ingestions WHERE ingestion_id=:id"; }
  public Mono<MediaIngestion> find(UUID id) { return db.sql(select()).bind("id",id).map((r,m)->map(r)).one(); }
  public Mono<MediaIngestion> findByKey(String actor,String key) { return db.sql("SELECT * FROM media_ingestions WHERE actor_id=:a AND idempotency_key=:k").bind("a",actor).bind("k",key).map((r,m)->map(r)).one(); }
  public Mono<MediaIngestion> findByUploadId(String uploadId) { return db.sql("SELECT * FROM media_ingestions WHERE upload_id=:u").bind("u",uploadId).map((r,m)->map(r)).one(); }
  public Mono<MediaIngestion> findByStorageId(long storageId) { return db.sql("SELECT * FROM media_ingestions WHERE storage_id=:s").bind("s",storageId).map((r,m)->map(r)).one(); }
  public Mono<MediaIngestion> insert(MediaIngestion i) { var s=db.sql("INSERT INTO media_ingestions(ingestion_id,actor_id,phase,version,retry_count,created_at,updated_at,next_attempt_at,idempotency_key,file_name,file_size,mime_type,correlation_id,storage_id) VALUES(:id,:a,:p,:v,0,:c,:u,:n,:k,:f,:s,:m,:id,:sid) RETURNING *"); s=bind(s,"id",i.ingestionId(),UUID.class); s=bind(s,"a",i.actorId(),String.class); s=bind(s,"p",i.phase().name(),String.class); s=bind(s,"v",i.version(),Long.class); s=bind(s,"c",i.createdAt(),Instant.class); s=bind(s,"u",i.updatedAt(),Instant.class); s=bind(s,"n",i.nextAttemptAt(),Instant.class); s=bind(s,"k",i.idempotencyKey(),String.class); s=bind(s,"f",i.fileName(),String.class); s=bind(s,"s",i.fileSize(),Long.class); s=bind(s,"m",i.mimeType(),String.class); s=bind(s,"id",i.ingestionId(),UUID.class); s=bind(s,"sid",i.storageId(),Long.class); return s.map((r,m)->map(r)).one(); }
  public Mono<Boolean> compareAndSet(MediaIngestion e,MediaIngestion n) { var s=db.sql("UPDATE media_ingestions SET catalog_item_id=:c,upload_id=:u,storage_id=:sid,upload_url=:url,phase=:p,failure_code=:f,version=:nv,updated_at=:now,retry_count=:r,next_attempt_at=:next WHERE ingestion_id=:id AND version=:ov"); s=bind(s,"c",n.catalogItemId(),Long.class); s=bind(s,"u",n.uploadId(),String.class); s=bind(s,"sid",n.storageId(),Long.class); s=bind(s,"url",n.uploadUrl(),String.class); s=bind(s,"p",n.phase().name(),String.class); s=bind(s,"f",n.failureCode(),String.class); s=bind(s,"nv",n.version(),Long.class); s=bind(s,"now",n.updatedAt(),Instant.class); s=bind(s,"r",n.retryCount(),Integer.class); s=bind(s,"next",n.nextAttemptAt(),Instant.class); return bind(s,"id",e.ingestionId(),UUID.class).bind("ov",e.version()).fetch().rowsUpdated().map(x->x==1); }
}
