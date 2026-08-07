package com.guille.media.reproductor.uploader.storage.infrastructure.database.storage;

import com.guille.media.reproductor.uploader.storage.domain.models.StoreObject;
import com.guille.media.reproductor.uploader.storage.domain.ports.StorageRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.r2dbc.core.DatabaseClient;

import reactor.core.publisher.Mono;

@RequiredArgsConstructor
public class SpringDataStorageRepository implements StorageRepository {

    private final DatabaseClient databaseClient;
    private final StorageMapper storageMapper;

    @Override
    public Mono<StoreObject> save(StoreObject storageObject) {
        StoreObjectJpaEntity entity = this.storageMapper.toEntity(storageObject);
        return this.databaseClient
                .sql(
                        """
			INSERT INTO store_objects (
				storage_id,
				object_key,
				status,
				content_type,
				content_length,
				checksum,
				last_modified_at
			)
			VALUES (
				:storageId,
				:objectKey,
				:status,
				:contentType,
				:contentLength,
				:checksum,
				:lastModifiedAt
			)
			RETURNING *
			""")
                .bindProperties(entity)
                .mapProperties(StoreObjectJpaEntity.class)
                .one()
                .map(this.storageMapper::toDomain);
    }

    @Override
    public Mono<StoreObject> findById(Long storageId) {
		return this.databaseClient
			.sql(
				"""
		SELECT * FROM users WHERE id = :user_id
		""")
			.bind("user_id", storageId)
			.mapProperties(StoreObjectJpaEntity.class)
			.one()
			.map(this.storageMapper::toDomain);
    }
}
