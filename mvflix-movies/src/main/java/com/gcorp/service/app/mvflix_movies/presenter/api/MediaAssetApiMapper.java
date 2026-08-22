package com.gcorp.service.app.mvflix_movies.presenter.api;

import com.gcorp.service.app.mvflix_movies.library.domain.MediaAsset;
import com.gcorp.service.app.mvflix_movies.library.domain.ScannedFile;
import com.gcorp.service.app.mvflix_movies.presenter.api.dto.MediaAssetResponse;
import com.gcorp.service.app.mvflix_movies.presenter.api.dto.ScanLibraryRequest;

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
                asset.getMovieId() == null ? null : asset.getMovieId().value());
    }
}
