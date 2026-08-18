package com.guille.media.reproductor.uploader.storage.infrastructure.database.storage;

import com.guille.media.reproductor.uploader.storage.domain.models.MediaLibrary;
import com.guille.media.reproductor.uploader.storage.domain.models.MediaLibraryType;

import org.springframework.stereotype.Component;

@Component
public class MediaLibraryMapper {

    public MediaLibraryJpaEntity toEntity(MediaLibrary library) {
        return MediaLibraryJpaEntity.builder()
                .id(library.getId())
                .type(library.getType().name())
                .rootPath(library.getRootPath())
                .enabled(library.isEnabled())
                .createdAt(library.getCreatedAt())
                .build();
    }

    public MediaLibrary toDomain(MediaLibraryJpaEntity entity) {
        return new MediaLibrary(
                entity.getId(),
                MediaLibraryType.valueOf(entity.getType()),
                entity.getRootPath(),
                Boolean.TRUE.equals(entity.getEnabled()),
                entity.getCreatedAt());
    }
}