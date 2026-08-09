package com.guille.media.bff.app.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record UploadListItem(
    Long storageId, String objectKey, String status, long sizeInBytes, String createdAt) {}