package com.gcorp.service.app.mvflix_media_ingestion.infrastructure.persistence;
import com.gcorp.service.app.mvflix_media_ingestion.application.InboxRepository; import org.springframework.r2dbc.core.DatabaseClient; import org.springframework.stereotype.Repository; import reactor.core.publisher.Mono; import java.util.UUID;
@Repository public class DatabaseInboxRepository implements InboxRepository { private final DatabaseClient db; public DatabaseInboxRepository(DatabaseClient db){this.db=db;}
 public Mono<Boolean> receive(UUID id,String type){return db.sql("INSERT INTO media_ingestion_inbox(event_id,event_type,status) VALUES(:id,:t,'RECEIVED') ON CONFLICT(event_id) DO NOTHING").bind("id",id).bind("t",type).fetch().rowsUpdated().map(x->x==1);}
 public Mono<Boolean> completed(UUID id){return db.sql("SELECT status FROM media_ingestion_inbox WHERE event_id=:id").bind("id",id).map((r,m)->"COMPLETED".equals(r.get("status",String.class))).one().defaultIfEmpty(false);}
 public Mono<Void> markCompleted(UUID id){return db.sql("UPDATE media_ingestion_inbox SET status='COMPLETED',completed_at=now(),last_error=NULL WHERE event_id=:id AND status<>'COMPLETED'").bind("id",id).fetch().rowsUpdated().then();}
 public Mono<Void> markFailed(UUID id,String error){return db.sql("UPDATE media_ingestion_inbox SET status='FAILED',last_error=:e WHERE event_id=:id").bind("id",id).bind("e",error).fetch().rowsUpdated().then();}
}
