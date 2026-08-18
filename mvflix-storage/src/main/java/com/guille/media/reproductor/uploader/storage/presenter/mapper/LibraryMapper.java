package com.guille.media.reproductor.uploader.storage.presenter.mapper;

import com.guille.media.reproductor.uploader.storage.domain.models.MediaLibrary;
import com.guille.media.reproductor.uploader.storage.domain.vos.DiscoveredFile;
import com.guille.media.reproductor.uploader.storage.presenter.dto.response.DiscoveredFileResponse;
import com.guille.media.reproductor.uploader.storage.presenter.dto.response.LibraryResponse;

import org.springframework.stereotype.Component;

@Component
public class LibraryMapper {

    public LibraryResponse toLibraryResponse(MediaLibrary library) {
        return new LibraryResponse(
                library.getId(),
                library.getType().name(),
                library.isEnabled(),
                library.getRootPath());
    }

    public DiscoveredFileResponse toDiscoveredFileResponse(DiscoveredFile file) {
        return new DiscoveredFileResponse(file.relativePath(), file.size(), file.mimeType());
    }
}