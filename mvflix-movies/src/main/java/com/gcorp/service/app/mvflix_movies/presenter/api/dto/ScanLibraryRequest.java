package com.gcorp.service.app.mvflix_movies.presenter.api.dto;

import java.util.List;

/** Archivos descubiertos por el scan de una biblioteca del operador. */
public record ScanLibraryRequest(List<ScannedFileItem> files) {

    public record ScannedFileItem(String relativePath, long size, String mimeType) {}
}
