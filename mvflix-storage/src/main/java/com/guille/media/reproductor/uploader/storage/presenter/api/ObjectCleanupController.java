package com.guille.media.reproductor.uploader.storage.presenter.api;

import com.guille.media.reproductor.uploader.storage.app.service.ObjectCleanupService;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import reactor.core.publisher.Mono;

@RestController
@RequestMapping(value = "/api/v1/movie/storage", produces = MediaType.APPLICATION_JSON_VALUE)
public class ObjectCleanupController {

  private final ObjectCleanupService objectCleanupService;

  public ObjectCleanupController(ObjectCleanupService objectCleanupService) {
    this.objectCleanupService = objectCleanupService;
  }

  @DeleteMapping(value = "/{storageId}")
  public Mono<ResponseEntity<Void>> deleteObject(@PathVariable Long storageId) {
    return this.objectCleanupService
        .deleteObject(storageId)
        .thenReturn(ResponseEntity.noContent().build());
  }
}