package com.guille.media.bff.app.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record UploadStatusDto(
    String uploadId,
    String storageKey,
    String status,
    ExpectedObjectData object) {

  @JsonIgnoreProperties(ignoreUnknown = true)
  public record ExpectedObjectData(long expectedSize, String expectedMime) {}
}