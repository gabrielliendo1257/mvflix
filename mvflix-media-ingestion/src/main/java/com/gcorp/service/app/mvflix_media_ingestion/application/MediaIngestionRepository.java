package com.gcorp.service.app.mvflix_media_ingestion.application;

import com.gcorp.service.app.mvflix_media_ingestion.domain.MediaIngestion;
import reactor.core.publisher.Mono;
import java.util.UUID;

public interface MediaIngestionRepository {
  Mono<MediaIngestion> find(UUID id);
  Mono<MediaIngestion> findByKey(String actor, String key);
  Mono<MediaIngestion> findByUploadId(String uploadId);
  Mono<MediaIngestion> findByStorageId(long storageId);
  Mono<MediaIngestion> insert(MediaIngestion ingestion);
  Mono<Boolean> compareAndSet(MediaIngestion expected, MediaIngestion replacement);
}
