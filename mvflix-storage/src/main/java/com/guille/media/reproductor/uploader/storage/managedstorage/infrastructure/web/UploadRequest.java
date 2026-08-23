package com.guille.media.reproductor.uploader.storage.managedstorage.infrastructure.web;

import com.fasterxml.jackson.annotation.JsonProperty;

public record UploadRequest(
    @JsonProperty(value = "filename", required = true) String filename,
    @JsonProperty(value = "file_size", required = true) long size,
    @JsonProperty(value = "mime_type", required = true) String mimeType) {}
