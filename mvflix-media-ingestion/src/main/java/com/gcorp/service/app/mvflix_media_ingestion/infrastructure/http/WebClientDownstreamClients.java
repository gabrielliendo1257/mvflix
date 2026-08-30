package com.gcorp.service.app.mvflix_media_ingestion.infrastructure.http;

import com.gcorp.service.app.mvflix_media_ingestion.application.DownstreamClients;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import java.util.Map;

@Component
public class WebClientDownstreamClients implements DownstreamClients {
  private final WebClient movies, storage;
  public WebClientDownstreamClients(@Value("${mvflix.downstream.movies-url:http://localhost:4040}") String m,@Value("${mvflix.downstream.storage-url:http://localhost:6060}") String s,WebClient.Builder b) { movies=b.baseUrl(m).build(); storage=b.baseUrl(s).build(); }
  public Mono<Long> createCatalogDraft(Map<String,Object> draft,String actor,String key) { return movies.post().uri("/api/v1/movies/identified-drafts").contentType(MediaType.APPLICATION_JSON).header("X-Actor-Id",actor).header("Idempotency-Key",key).bodyValue(draft).retrieve().bodyToMono(Map.class).map(x->((Number)x.get("id")).longValue()); }
  public Mono<Upload> prepareUpload(String name,long size,String mime,String actor,String key) { return storage.post().uri("/api/v1/movie/storage/upload").contentType(MediaType.APPLICATION_JSON).header("X-Actor-Id",actor).bodyValue(Map.of("filename",name,"file_size",size,"mime_type",mime,"idempotency_key",key)).retrieve().bodyToMono(Map.class).map(x->new Upload(String.valueOf(x.get("uploadId")),String.valueOf(x.get("storageKey")),String.valueOf(x.get("uploadUrl")))); }
  public Mono<Void> requestUploadCompletion(String uploadId,String actor,String idempotencyKey) { return storage.post().uri("/api/v1/movie/storage/upload/{uploadId}/complete",uploadId).header("X-Actor-Id",actor).header("Idempotency-Key",idempotencyKey).retrieve().bodyToMono(Void.class); }
  public Mono<Void> completeCatalog(long id,String objectKey,long objectId,String actor) { return movies.post().uri("/api/v1/movies/{id}/complete",id).contentType(MediaType.APPLICATION_JSON).header("X-Actor-Id",actor).bodyValue(Map.of("object_id",objectId,"object_key",objectKey)).retrieve().bodyToMono(Void.class); }
  public Mono<Void> cancelUpload(String id,String actor,String key) { return storage.post().uri("/api/v1/movie/storage/upload/{id}/cancel",id).header("X-Actor-Id",actor).header("Idempotency-Key",key).retrieve().bodyToMono(Void.class); }
}
