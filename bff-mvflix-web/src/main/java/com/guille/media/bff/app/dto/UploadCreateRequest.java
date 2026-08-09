package com.guille.media.bff.app.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record UploadCreateRequest(
    @JsonProperty(value = "filename", required = true) String filename,
    @JsonProperty(value = "file_size", required = true) long size,
    @JsonProperty(value = "mime_type", required = true) String mimeType) {}