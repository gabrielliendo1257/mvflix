package com.guille.media.reproductor.uploader.storage.managedstorage.infrastructure.objectstore;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import com.guille.media.reproductor.uploader.storage.managedstorage.domain.port.ObjectStorageService;
import com.guille.media.reproductor.uploader.storage.managedstorage.domain.model.BucketName;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Crea el bucket dedicado de usuarios (idempotente) al arrancar la aplicación.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BucketInitializer implements ApplicationRunner {

  private final ObjectStorageService objectStorageService;

  @Value("${minio.bucket}")
  private String bucket;

  @Override
  public void run(ApplicationArguments args) {
    this.objectStorageService
        .ensureBucket(BucketName.of(this.bucket))
        .doOnSuccess(v -> log.info("Bucket '{}' ensured on startup", this.bucket))
        .subscribe(
            v -> {}, error -> log.error("Error ensuring bucket '{}' on startup", this.bucket, error));
  }
}