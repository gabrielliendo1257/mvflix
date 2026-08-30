package com.gcorp.service.app.mvflix_media_ingestion.infrastructure.web;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.gcorp.service.app.mvflix_media_ingestion.application.MediaIngestionService;
import com.gcorp.service.app.mvflix_media_ingestion.domain.MediaIngestion;
import jakarta.validation.Valid; import jakarta.validation.constraints.*;
import org.springframework.http.*; import org.springframework.security.core.annotation.AuthenticationPrincipal; import org.springframework.security.oauth2.jwt.Jwt; import org.springframework.web.bind.annotation.*; import reactor.core.publisher.Mono; import java.util.*;

@RestController @RequestMapping(value="/api/v1/ingestions", produces=MediaType.APPLICATION_JSON_VALUE)
public class MediaIngestionController {
  private final MediaIngestionService service; public MediaIngestionController(MediaIngestionService service){this.service=service;}
  public record File(@NotBlank String filename,@Positive @JsonProperty("file_size") long fileSize,@NotBlank @JsonProperty("mime_type") String mimeType){}
  public record Create(@NotNull Map<String,Object> draft,@Valid @NotNull File file){}
  @PostMapping public Mono<ResponseEntity<MediaIngestion>> create(@RequestHeader("Idempotency-Key") String key,@Valid @RequestBody Create r,@AuthenticationPrincipal Jwt jwt){return service.create(actor(jwt),key,r.draft(),r.file().filename(),r.file().fileSize(),r.file().mimeType()).map(ResponseEntity::ok);}
  @GetMapping("/{id}") public Mono<ResponseEntity<MediaIngestion>> get(@PathVariable UUID id,@AuthenticationPrincipal Jwt jwt){return service.get(id,actor(jwt)).map(ResponseEntity::ok).defaultIfEmpty(ResponseEntity.notFound().build());}
  public record Complete(@NotNull @JsonProperty("object_id") Long objectId,@NotBlank @JsonProperty("object_key") String objectKey){}
  @PostMapping("/{id}/complete") public Mono<ResponseEntity<MediaIngestion>> complete(@PathVariable UUID id,@Valid @RequestBody Complete r,@AuthenticationPrincipal Jwt jwt){return service.complete(id,actor(jwt),r.objectId(),r.objectKey()).map(ResponseEntity::ok).defaultIfEmpty(ResponseEntity.notFound().build());}
  @PostMapping("/{id}/cancel") public Mono<ResponseEntity<MediaIngestion>> cancel(@PathVariable UUID id,@AuthenticationPrincipal Jwt jwt){return service.cancel(id,actor(jwt)).map(ResponseEntity::ok).defaultIfEmpty(ResponseEntity.notFound().build());}
  private String actor(Jwt jwt){if(jwt==null||jwt.getSubject()==null)throw new IllegalStateException("JWT subject required"); return jwt.getSubject();}
}
