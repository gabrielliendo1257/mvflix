package com.guille.media.reproductor.uploader.storage.infrastructure.database.storage;

import com.guille.media.reproductor.uploader.storage.domain.models.StoreObject;
import com.guille.media.reproductor.uploader.storage.domain.ports.StorageRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.r2dbc.core.DatabaseClient;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;

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
				owner_username,
				object_key,
				status,
				content_type,
				content_length,
				checksum,
				created_at,
				last_modified_at
			)
			VALUES (
				:ownerUsername,
				:objectKey,
				:status,
				:contentType,
				:contentLength,
				:checksum,
				:createdAt,
				:lastModifiedAt
			)
			RETURNING *
			""")
                .bind("ownerUsername", entity.getOwnerUsername())
                .bind("objectKey", entity.getObjectKey())
                .bind("status", entity.getStatus())
                .bind("contentType", entity.getContentType())
                .bind("contentLength", entity.getContentLength())
                .bind("checksum", entity.getChecksum())
                .bind("createdAt", entity.getCreatedAt())
                .bind("lastModifiedAt", entity.getLastModifiedAt())
                .mapProperties(StoreObjectJpaEntity.class)
                .one()
                .map(this.storageMapper::toDomain);
    }

    @Override
    public Mono<StoreObject> findById(Long storageId) {
		return this.databaseClient
			.sql(
				"""
		SELECT * FROM store_objects WHERE storage_id = :storage_id
		""")
			.bind("storage_id", storageId)
			.mapProperties(StoreObjectJpaEntity.class)
			.one()
			.map(this.storageMapper::toDomain);
    }

    @Override
    public Mono<StoreObject> markCompleted(Long storageId) {
        return this.databaseClient
                .sql(
                        """
		UPDATE store_objects
		SET status = 'COMPLETED'
		WHERE storage_id = :storage_id
		  AND status = 'PENDING'
		RETURNING *
		""")
                .bind("storage_id", storageId)
                .mapProperties(StoreObjectJpaEntity.class)
                .one()
                .map(this.storageMapper::toDomain);
    }

    @Override
    public Mono<StoreObject> markExpired(Long storageId) {
        return this.databaseClient
                .sql(
                        """
		UPDATE store_objects
		SET status = 'EXPIRED'
		WHERE storage_id = :storage_id
		  AND status = 'PENDING'
		RETURNING *
		""")
                .bind("storage_id", storageId)
                .mapProperties(StoreObjectJpaEntity.class)
                .one()
                .map(this.storageMapper::toDomain);
    }

    @Override
    public Flux<StoreObject> findPendingCreatedBefore(Instant cutoff) {
        return this.databaseClient
                .sql(
                        """
		SELECT * FROM store_objects
		WHERE status = 'PENDING'
		  AND created_at < :cutoff
		""")
                .bind("cutoff", cutoff)
                .mapProperties(StoreObjectJpaEntity.class)
                .all()
                .map(this.storageMapper::toDomain);
    }
}
