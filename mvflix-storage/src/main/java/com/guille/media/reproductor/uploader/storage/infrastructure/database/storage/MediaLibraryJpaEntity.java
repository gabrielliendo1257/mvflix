package com.guille.media.reproductor.uploader.storage.infrastructure.database.storage;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;

@Data
@Table(name = "media_libraries")
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MediaLibraryJpaEntity {

    @Id private Long id;

    private String type;

    private String rootPath;

    private Boolean enabled;

    private Instant createdAt;
}