package com.gcorp.service.app.mvflix_movies.presenter.api.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.util.List;

/** Archivos descubiertos por el scan de una biblioteca del operador. */
public record ScanLibraryRequest(
        @NotNull @Size(max = 10_000) List<@Valid ScannedFileItem> files) {

    public record ScannedFileItem(
            @NotBlank String relativePath,
            @PositiveOrZero long size,
            @NotBlank String mimeType) {}
}
