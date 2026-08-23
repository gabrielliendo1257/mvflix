package com.gcorp.service.app.mvflix_movies.library.infrastructure.web;

import com.gcorp.service.app.mvflix_movies.library.domain.MediaAsset;
import com.gcorp.service.app.mvflix_movies.library.domain.ScannedFile;
import com.gcorp.service.app.mvflix_movies.library.infrastructure.web.dto.MediaAssetResponse;
import com.gcorp.service.app.mvflix_movies.library.infrastructure.web.dto.ScanLibraryRequest;

import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class MediaAssetApiMapper {

    public List<ScannedFile> toScannedFiles(ScanLibraryRequest request) {
        if (request.files() == null) {
            return List.of();
        }
        return request.files().stream()
                .map(file -> new ScannedFile(file.relativePath(), file.size(), file.mimeType()))
                .toList();
    }

    public MediaAssetResponse toResponse(MediaAsset asset) {
        return new MediaAssetResponse(
                asset.getId().value(),
                asset.getLibraryId(),
                asset.getRelativePath(),
                asset.getSize(),
                asset.getMimeType(),
                asset.getStatus().name(),
                asset.getCatalogItemId() == null ? null : asset.getCatalogItemId().value());
    }
}
