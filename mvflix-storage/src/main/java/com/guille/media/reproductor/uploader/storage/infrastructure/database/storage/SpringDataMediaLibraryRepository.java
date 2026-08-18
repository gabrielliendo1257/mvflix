package com.guille.media.reproductor.uploader.storage.infrastructure.database.storage;

import com.guille.media.reproductor.uploader.storage.domain.models.MediaLibrary;
import com.guille.media.reproductor.uploader.storage.domain.ports.MediaLibraryRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Repository;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
@RequiredArgsConstructor
public class SpringDataMediaLibraryRepository implements MediaLibraryRepository {

    private final DatabaseClient databaseClient;
    private final MediaLibraryMapper mapper;

    @Override
    public Mono<MediaLibrary> findById(Long id) {
        return this.databaseClient
                .sql("SELECT * FROM media_libraries WHERE id = :id")
                .bind("id", id)
                .mapProperties(MediaLibraryJpaEntity.class)
                .one()
                .map(this.mapper::toDomain);
    }

    @Override
    public Mono<MediaLibrary> findByRootPath(String rootPath) {
        return this.databaseClient
                .sql("SELECT * FROM media_libraries WHERE root_path = :root_path")
                .bind("root_path", rootPath)
                .mapProperties(MediaLibraryJpaEntity.class)
                .one()
                .map(this.mapper::toDomain);
    }

    @Override
    public Flux<MediaLibrary> findAllEnabled() {
        return this.databaseClient
                .sql("SELECT * FROM media_libraries WHERE enabled = TRUE ORDER BY id")
                .mapProperties(MediaLibraryJpaEntity.class)
                .all()
                .map(this.mapper::toDomain);
    }

    @Override
    public Mono<MediaLibrary> save(MediaLibrary library) {
        MediaLibraryJpaEntity entity = this.mapper.toEntity(library);
        var spec =
                this.databaseClient
                        .sql(
                                """
                                INSERT INTO media_libraries (type, root_path, enabled, created_at)
                                VALUES (:type, :root_path, :enabled, :created_at)
                                ON CONFLICT (root_path) DO UPDATE SET enabled = EXCLUDED.enabled
                                RETURNING *
                                """)
                        .bind("type", entity.getType())
                        .bind("root_path", entity.getRootPath())
                        .bind("enabled", entity.getEnabled())
                        .bind("created_at", entity.getCreatedAt());
        return spec.mapProperties(MediaLibraryJpaEntity.class)
                .one()
                .map(this.mapper::toDomain);
    }
}