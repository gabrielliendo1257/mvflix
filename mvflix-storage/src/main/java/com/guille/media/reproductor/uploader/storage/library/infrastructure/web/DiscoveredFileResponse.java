package com.guille.media.reproductor.uploader.storage.library.infrastructure.web;

public record DiscoveredFileResponse(String relativePath, long size, String mimeType) {}