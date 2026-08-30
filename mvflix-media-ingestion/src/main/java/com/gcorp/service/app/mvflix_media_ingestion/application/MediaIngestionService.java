package com.gcorp.service.app.mvflix_media_ingestion.application;

import com.gcorp.service.app.mvflix_media_ingestion.domain.MediaIngestion;
import com.gcorp.service.app.mvflix_media_ingestion.domain.MediaIngestion.Phase;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import java.time.Instant;
import java.util.*;

@Service
public class MediaIngestionService {
  private final MediaIngestionRepository repository; private final DownstreamClients clients; private final Outbox outbox; private final CompensationRepository compensations;
  public MediaIngestionService(MediaIngestionRepository repository, DownstreamClients clients, Outbox outbox, CompensationRepository compensations) { this.repository=repository; this.clients=clients; this.outbox=outbox; this.compensations=compensations; }
  public Mono<MediaIngestion> create(String actor, String key, Map<String,Object> draft, String fileName, long size, String mime) {
    return repository.findByKey(actor,key).switchIfEmpty(Mono.defer(() -> {
      var now=Instant.now(); var i=new MediaIngestion(UUID.randomUUID(),actor,null,null,Phase.STARTING,null,0,0,now,now,now,key,fileName,size,mime,null);
       return repository.insert(i).flatMap(saved -> step(saved, Phase.PREPARING_CATALOG, null, null)
         .then(clients.createCatalogDraft(draft,actor,saved.ingestionId()+":create-catalog-draft"))
         .flatMap(catalog -> repository.find(saved.ingestionId()).flatMap(current -> step(current,Phase.PREPARING_UPLOAD,catalog,null)))
         .flatMap(x -> prepare(x,fileName,size,mime)).onErrorResume(e -> fail(saved,e)));
    }));
  }
  private Mono<MediaIngestion> prepare(MediaIngestion i,String name,long size,String mime) {
    return clients.prepareUpload(name,size,mime,i.actorId(),i.ingestionId()+":prepare-upload").flatMap(u -> {
      var n=new MediaIngestion(i.ingestionId(),i.actorId(),i.catalogItemId(),u.uploadId(),Phase.AWAITING_UPLOAD,null,i.version()+1,i.retryCount(),i.createdAt(),Instant.now(),i.nextAttemptAt(),i.idempotencyKey(),i.fileName(),i.fileSize(),i.mimeType(),u.uploadUrl(),null);
      return repository.compareAndSet(i,n).flatMap(ok -> ok ? outbox.started(n).thenReturn(n) : Mono.error(new IllegalStateException("CAS failed")));
    }).onErrorResume(e -> fail(i,e));
  }
  public Mono<MediaIngestion> get(UUID id,String actor) { return repository.find(id).filter(i -> i.actorId().equals(actor)); }
  public Mono<MediaIngestion> cancel(UUID id,String actor) { return get(id,actor).flatMap(i -> { if(i.phase()==Phase.CANCELLED||i.phase()==Phase.COMPLETED)return Mono.just(i); var n=i.transition(Phase.CANCELLING,null,null,null); return repository.compareAndSet(i,n).flatMap(ok -> ok ? (i.uploadId()==null?Mono.empty():clients.cancelUpload(i.uploadId(),actor,id+":cancel-upload")).then(Mono.defer(() -> repository.compareAndSet(n,n.transition(Phase.CANCELLED,null,null,null)))).then(repository.find(id)).flatMap(x -> outbox.cancelled(x).thenReturn(x)) : Mono.error(new IllegalStateException("CAS failed"))); }).onErrorResume(e -> get(id,actor).flatMap(i -> fail(i,e)));
  }
  public Mono<MediaIngestion> complete(UUID id,String actor,long objectId,String objectKey) { return get(id,actor).flatMap(i -> finalize(i,objectId,objectKey)); }
  public Mono<Void> uploadCompleted(UUID id,long objectId,String objectKey,String causation) { return repository.find(id).switchIfEmpty(Mono.error(new IllegalArgumentException("unknown correlationId"))).flatMap(i -> finalize(i,objectId,objectKey).then()).then(); }
  public Mono<Void> uploadCompletedByUploadId(String uploadId,long objectId,String objectKey,String causation) { return repository.findByUploadId(uploadId).switchIfEmpty(Mono.error(new IllegalArgumentException("unknown uploadId"))).flatMap(i -> finalize(i,objectId,objectKey).then()).then(); }
  public Mono<Void> uploadCompletedByStorageId(long storageId,long objectId,String objectKey) { return repository.findByStorageId(storageId).switchIfEmpty(Mono.error(new IllegalArgumentException("unknown storageId"))).flatMap(i -> finalize(i,objectId,objectKey).then()).then(); }
  private Mono<MediaIngestion> step(MediaIngestion i,Phase phase,Long catalog,String upload) { return repository.compareAndSet(i,i.transition(phase,catalog,upload,null)).flatMap(ok -> ok ? repository.find(i.ingestionId()) : Mono.error(new IllegalStateException("CAS failed"))); }
  private Mono<MediaIngestion> fail(MediaIngestion i,Throwable e) { return repository.find(i.ingestionId()).flatMap(current -> { if(current.phase()==Phase.COMPLETED||current.phase()==Phase.CANCELLED)return Mono.just(current); var x=current.failed(e.getClass().getSimpleName()+":"+String.valueOf(e.getMessage())); Mono<Void> cleanup=compensations==null?Mono.empty():Mono.when(current.uploadId()!=null?compensations.schedule(x.ingestionId(),"CANCEL_UPLOAD"):Mono.empty(),current.catalogItemId()!=null?compensations.schedule(x.ingestionId(),"DISCARD_DRAFT"):Mono.empty()).then(); return repository.compareAndSet(current,x).flatMap(ok -> ok ? cleanup.then(repository.find(current.ingestionId())) : Mono.error(new IllegalStateException("CAS failed"))).flatMap(saved -> outbox.failed(saved).thenReturn(saved)); }); }
  private Mono<MediaIngestion> finalize(MediaIngestion i,long objectId,String objectKey) { if(i.phase()==Phase.COMPLETED)return Mono.just(i); if(i.phase()!=Phase.AWAITING_UPLOAD && i.phase()!=Phase.FINALIZING_CATALOG)return Mono.error(new IllegalStateException("ingestion not awaiting upload")); var n=i.phase()==Phase.FINALIZING_CATALOG?i:i.transition(Phase.FINALIZING_CATALOG,null,null,null); return (i.phase()==Phase.FINALIZING_CATALOG?Mono.just(true):repository.compareAndSet(i,n)).flatMap(ok -> ok ? clients.completeCatalog(n.catalogItemId(),objectKey,objectId,n.actorId()).then(repository.compareAndSet(n,n.transition(Phase.COMPLETED,null,null,null))).then(repository.find(i.ingestionId())).flatMap(x -> outbox.completed(x).thenReturn(x)) : Mono.error(new IllegalStateException("CAS failed"))).onErrorResume(e -> reconcile(n,e)); }
  private Mono<MediaIngestion> reconcile(MediaIngestion i,Throwable e) { var n=new MediaIngestion(i.ingestionId(),i.actorId(),i.catalogItemId(),i.uploadId(),Phase.RECONCILIATION_REQUIRED,e.toString(),i.version()+1,i.retryCount()+1,i.createdAt(),Instant.now(),Instant.now().plusSeconds(60),i.idempotencyKey(),i.fileName(),i.fileSize(),i.mimeType(),i.uploadUrl(),i.storageId()); Mono<Void> cleanup=Mono.when(i.uploadId()!=null?compensations.schedule(i.ingestionId(),"CANCEL_UPLOAD"):Mono.empty(),i.catalogItemId()!=null?compensations.schedule(i.ingestionId(),"DISCARD_DRAFT"):Mono.empty()); return repository.compareAndSet(i,n).flatMap(ok->ok?cleanup.then(outbox.failed(n)).thenReturn(n):Mono.error(new IllegalStateException("CAS failed"))); }
}
