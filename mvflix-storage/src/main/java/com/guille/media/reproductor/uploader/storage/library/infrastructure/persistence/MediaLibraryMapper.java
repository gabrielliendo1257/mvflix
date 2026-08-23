package com.guille.media.reproductor.uploader.storage.library.infrastructure.persistence;

import com.guille.media.reproductor.uploader.storage.library.domain.model.MediaLibrary;
import com.guille.media.reproductor.uploader.storage.library.domain.model.MediaLibraryType;

import org.springframework.stereotype.Component;

@Component
public class MediaLibraryMapper {

    public MediaLibraryJpaEntity toEntity(MediaLibrary library) {
        return MediaLibraryJpaEntity.builder()
                .id(library.getId())
                .type(library.getType().name())
                .rootPath(library.getRootPath())
                .enabled(library.isEnabled())
                .ownerUsername(library.getOwnerUsername())
                .createdAt(library.getCreatedAt())
                .build();
    }

    public MediaLibrary toDomain(MediaLibraryJpaEntity entity) {
        return new MediaLibrary(
                entity.getId(),
                MediaLibraryType.valueOf(entity.getType()),
                entity.getRootPath(),
                Boolean.TRUE.equals(entity.getEnabled()),
                entity.getOwnerUsername(),
                entity.getCreatedAt());
    }
}