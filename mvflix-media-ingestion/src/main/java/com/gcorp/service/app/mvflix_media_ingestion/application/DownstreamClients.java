package com.gcorp.service.app.mvflix_media_ingestion.application;

import java.util.Map;
import reactor.core.publisher.Mono;

public interface DownstreamClients {
  Mono<Long> createCatalogDraft(Map<String, Object> draft, String actor, String key, String correlationId);

  Mono<Upload> prepareUpload(
      String fileName, long fileSize, String mimeType, String actor, String key);

  Mono<Void> requestUploadCompletion(String uploadId, String actor, String idempotencyKey);

  Mono<Void> completeCatalog(long catalogItemId, String objectKey, long objectId, String actor);

  Mono<Void> discardDraft(long catalogItemId, String actor, String idempotencyKey);

  Mono<Void> cancelUpload(String uploadId, String actor, String key);

  Mono<StorageStatus> storageStatus(String uploadId, String actor);

  Mono<CatalogStatus> catalogStatus(long catalogItemId, String actor);

  record Upload(String uploadId, String storageKey, String uploadUrl) {}

  record StorageStatus(String status, Long objectId, String objectKey) {}

  record CatalogStatus(String status) {}
}
