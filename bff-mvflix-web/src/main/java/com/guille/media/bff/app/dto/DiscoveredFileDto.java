package com.guille.media.bff.app.dto;

/** Archivo descubierto por el scanner del storage en una biblioteca LOCAL. */
public record DiscoveredFileDto(String relativePath, long size, String mimeType) {}
