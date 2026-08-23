package com.guille.media.reproductor.uploader.storage.library.infrastructure.web;

import com.guille.media.reproductor.uploader.storage.library.domain.model.MediaLibrary;
import com.guille.media.reproductor.uploader.storage.library.domain.model.DiscoveredFile;
import com.guille.media.reproductor.uploader.storage.library.infrastructure.web.DiscoveredFileResponse;
import com.guille.media.reproductor.uploader.storage.library.infrastructure.web.LibraryResponse;

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