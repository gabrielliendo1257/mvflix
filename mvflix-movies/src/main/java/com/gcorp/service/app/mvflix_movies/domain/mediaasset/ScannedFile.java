package com.gcorp.service.app.mvflix_movies.domain.mediaasset;

/** Archivo multimedia descubierto por el scanner del storage (biblioteca LOCAL). */
public record ScannedFile(String relativePath, long size, String mimeType) {}
