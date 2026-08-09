package com.guille.media.reproductor.uploader.storage.presenter.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Notificación de bucket event de MinIO/s3.
 *
 * <p>Se recibe de la config de webhook del object store cuando un objeto termina de subirse
 * ({@code s3:ObjectCreated:Put} o {@code s3:ObjectCreated:CompleteMultipartUpload}).
 */
public record MinioEventNotification(
    @JsonProperty("EventName") String eventName, @JsonProperty("Records") List<Record> records) {

  public List<Record> records() {
    return this.records == null ? List.of() : this.records;
  }

  public record Record(
      @JsonProperty("eventName") String eventName, @JsonProperty("s3") S3 s3) {

    public boolean isObjectCreated() {
      return this.eventName != null && this.eventName.startsWith("s3:ObjectCreated:");
    }

    public boolean isObjectRemoved() {
      return this.eventName != null && this.eventName.startsWith("s3:ObjectRemoved:");
    }

    public String objectKey() {
      return this.s3 != null && this.s3.object() != null ? this.s3.object().key() : null;
    }

    public record S3(@JsonProperty("object") ObjectInfo object) {

      public record ObjectInfo(@JsonProperty("key") String key, @JsonProperty("size") Long size) {}
    }
  }
}