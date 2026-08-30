package com.gcorp.service.app.mvflix_media_ingestion.application;

import reactor.core.publisher.Mono;
import java.util.Map;

public interface DownstreamClients {
  Mono<Long> createCatalogDraft(Map<String,Object> draft, String actor, String key);
  Mono<Upload> prepareUpload(String fileName, long fileSize, String mimeType, String actor, String key);
  Mono<Void> requestUploadCompletion(String uploadId, String actor, String idempotencyKey);
  Mono<Void> completeCatalog(long catalogItemId, String objectKey, long objectId, String actor);
  Mono<Void> cancelUpload(String uploadId, String actor, String key);
  record Upload(String uploadId, String storageKey, String uploadUrl) {}
}
