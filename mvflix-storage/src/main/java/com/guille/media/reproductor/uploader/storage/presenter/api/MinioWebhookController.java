package com.guille.media.reproductor.uploader.storage.presenter.api;

import com.guille.media.reproductor.uploader.storage.domain.service.UploadService;
import com.guille.media.reproductor.uploader.storage.presenter.dto.request.MinioEventNotification;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.security.MessageDigest;
import java.nio.charset.StandardCharsets;

/**
 * Webhook de bucket events del object store (MinIO).
 *
 * <p>MinIO es la fuente de verdad de que el objeto terminó de subirse; este endpoint reconcilia
 * el estado {@code COMPLETED} sin depender de que el cliente confirme. La autenticación usa el
 * token configurado en el target webhook de MinIO (header {@code X-Minio-Token}).
 */
@Slf4j
@RestController
@RequestMapping(path = "/internal/minio/events", produces = MediaType.APPLICATION_JSON_VALUE)
public class MinioWebhookController {

  private final UploadService uploadService;
  private final String webhookToken;

  public MinioWebhookController(
      UploadService uploadService, @Value("${minio.webhook-token:}") String webhookToken) {
    this.uploadService = uploadService;
    this.webhookToken = webhookToken;
  }

  @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
  public Mono<ResponseEntity<Void>> onObjectCreated(
      @RequestHeader(value = "X-Minio-Token", required = false) String token,
      @RequestBody MinioEventNotification notification) {
    if (!isAuthorized(token)) {
      log.warn("Rejected minio webhook event without a valid token");
      return Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());
    }

    return Flux.fromIterable(notification.records())
        .filter(MinioEventNotification.Record::isObjectCreated)
        .flatMap(
            record -> {
              final String key = record.objectKey();
              log.info("Object created event received from object store: key={}", key);
              return this.uploadService.completeUploadByKey(key);
            })
        .then()
        .thenReturn(ResponseEntity.ok().build());
  }

  private boolean isAuthorized(String token) {
    if (token == null || this.webhookToken.isEmpty()) {
      return false;
    }
    return MessageDigest.isEqual(
        token.getBytes(StandardCharsets.UTF_8),
        this.webhookToken.getBytes(StandardCharsets.UTF_8));
  }
}