package com.gcorp.service.app.mvflix_activity.infrastructure.persistence;

import com.gcorp.service.app.mvflix_activity.application.port.*;
import com.gcorp.service.app.mvflix_activity.application.ActivityQueryService.ActivityRecord;
import com.gcorp.service.app.mvflix_activity.domain.PlaybackProgressed;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.*;
import java.util.UUID;

@Repository
public class ActivityPersistence implements ActivityInbox, WatchActivityRepository {
  private final DatabaseClient db;
  public ActivityPersistence(DatabaseClient db) { this.db=db; }
  public Mono<Void> recordReceived(String id, String type) { return db.sql("INSERT INTO activity_inbox(event_id,event_type,status) VALUES(:id,:type,'RECEIVED') ON CONFLICT(event_id) DO NOTHING").bind("id",UUID.fromString(id)).bind("type",type).fetch().rowsUpdated().then(); }
  public Mono<Boolean> isCompleted(String id) { return db.sql("SELECT status FROM activity_inbox WHERE event_id=:id").bind("id",UUID.fromString(id)).map((r,m)->"COMPLETED".equals(r.get("status",String.class))).one().defaultIfEmpty(false); }
  public Mono<Void> markCompleted(String id) { return db.sql("UPDATE activity_inbox SET status='COMPLETED',completed_at=NOW(),updated_at=NOW(),last_error=NULL WHERE event_id=:id").bind("id",UUID.fromString(id)).fetch().rowsUpdated().then(); }
  public Mono<Void> markFailed(String id,String type,String error) { return db.sql("INSERT INTO activity_inbox(event_id,event_type,status,last_error) VALUES(:id,:type,'FAILED',:error) ON CONFLICT(event_id) DO UPDATE SET status='FAILED',last_error=:error,updated_at=NOW()").bind("id",UUID.fromString(id)).bind("type",type).bind("error",error).fetch().rowsUpdated().then(); }
  public Mono<Void> upsert(PlaybackProgressed e) {
    var q="""
      INSERT INTO watch_activity(owner_username,movie_id,media_id,position_seconds,duration_seconds,completed,sequence,last_watched_at,created_at,updated_at)
      VALUES(:owner,:movie,:media,:position,:duration,:completed,:sequence,NOW(),NOW(),NOW())
      ON CONFLICT(owner_username,movie_id,media_id_key) DO UPDATE SET position_seconds=EXCLUDED.position_seconds,duration_seconds=EXCLUDED.duration_seconds,completed=EXCLUDED.completed,sequence=EXCLUDED.sequence,last_watched_at=EXCLUDED.last_watched_at,updated_at=NOW()
      WHERE watch_activity.sequence < EXCLUDED.sequence""";
    var s=db.sql(q).bind("owner",e.ownerUsername()).bind("movie",e.movieId()).bind("position",e.positionSeconds()).bind("completed",e.completed()).bind("sequence",e.sequence());
    s=e.mediaId()==null?s.bindNull("media",Long.class):s.bind("media",e.mediaId()); s=e.durationSeconds()==null?s.bindNull("duration",Long.class):s.bind("duration",e.durationSeconds()); return s.fetch().rowsUpdated().then();
  }
  private Flux<ActivityRecord> query(String sql,String owner,Long movie,Integer limit) { DatabaseClient.GenericExecuteSpec s=db.sql(sql).bind("owner",owner); if(movie!=null)s=s.bind("movie",movie); if(limit!=null)s=s.bind("limit",limit); return s.map((r,m)->new ActivityRecord(r.get("movie_id",Long.class),r.get("media_id",Long.class),r.get("position_seconds",Long.class),r.get("duration_seconds",Long.class),r.get("completed",Boolean.class),r.get("sequence",Long.class),r.get("last_watched_at",java.time.Instant.class))).all(); }
  public Flux<ActivityRecord> history(String o,int l){return query("SELECT * FROM watch_activity WHERE owner_username=:owner ORDER BY last_watched_at DESC LIMIT :limit",o,null,l);}
  public Flux<ActivityRecord> continueWatching(String o,int l){return query("SELECT * FROM watch_activity WHERE owner_username=:owner AND completed=false ORDER BY last_watched_at DESC LIMIT :limit",o,null,l);}
  public Mono<ActivityRecord> movie(String o,long movie){return query("SELECT * FROM watch_activity WHERE owner_username=:owner AND movie_id=:movie ORDER BY last_watched_at DESC",o,movie,null).next();}
}
