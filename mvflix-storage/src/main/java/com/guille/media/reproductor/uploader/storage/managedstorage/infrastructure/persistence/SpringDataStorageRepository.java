package com.guille.media.reproductor.uploader.storage.managedstorage.infrastructure.persistence;

import com.guille.media.reproductor.uploader.storage.managedstorage.domain.exception.IllegalStateTransitionException;
import com.guille.media.reproductor.uploader.storage.managedstorage.domain.model.StorageObject;
import com.guille.media.reproductor.uploader.storage.managedstorage.domain.model.StorageObject.StorageSessionStatus;
import com.guille.media.reproductor.uploader.storage.managedstorage.domain.port.StorageRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Repository;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;

@Repository
@RequiredArgsConstructor
public class SpringDataStorageRepository implements StorageRepository {

    private final DatabaseClient databaseClient;
    private final StorageMapper storageMapper;

    @Override
    public Mono<StorageObject> save(StorageObject storageObject) {
        StorageObjectJpaEntity entity = this.storageMapper.toEntity(storageObject);
        var spec =
                this.databaseClient
                        .sql(
                                """
		 INSERT INTO store_objects (
				owner_username,
				idempotency_key,
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
				:idempotencyKey,
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
                        .bind("createdAt", entity.getCreatedAt())
                        .bind("lastModifiedAt", entity.getLastModifiedAt());

        if (entity.getChecksum() == null) {
            spec = spec.bindNull("checksum", String.class);
        } else {
            spec = spec.bind("checksum", entity.getChecksum());
        }

        if (entity.getIdempotencyKey() != null) {
            spec = spec.bind("idempotencyKey", entity.getIdempotencyKey());
        } else {
            spec = spec.bindNull("idempotencyKey", String.class);
        }
        return spec.mapProperties(StorageObjectJpaEntity.class)
                .one()
                .map(this.storageMapper::toDomain);
    }

    @Override
    public Mono<StorageObject> findByOwnerAndIdempotencyKey(String ownerUsername, String idempotencyKey) {
        return this.databaseClient.sql("SELECT * FROM store_objects WHERE owner_username = :owner "
                + "AND idempotency_key = :key")
            .bind("owner", ownerUsername).bind("key", idempotencyKey)
            .mapProperties(StorageObjectJpaEntity.class).one().map(this.storageMapper::toDomain);
    }

    @Override
    public Mono<StorageObject> findById(Long storageId) {
		return this.databaseClient
			.sql(
				"""
		SELECT * FROM store_objects WHERE storage_id = :storage_id
		""")
			.bind("storage_id", storageId)
			.mapProperties(StorageObjectJpaEntity.class)
			.one()
			.map(this.storageMapper::toDomain);
    }

    @Override
    public Mono<StorageObject> findByObjectKey(String objectKey) {
        return this.databaseClient
                .sql(
                        """
		SELECT * FROM store_objects
		WHERE object_key = :object_key
		""")
                .bind("object_key", objectKey)
                .mapProperties(StorageObjectJpaEntity.class)
                .one()
                .map(this.storageMapper::toDomain);
    }

    @Override
    public Mono<StorageObject> updateStatus(
        StorageObject storageObject, StorageSessionStatus expectedStatus) {
        return this.databaseClient
                .sql(
                        """
		UPDATE store_objects
		SET status = :status
		WHERE storage_id = :storage_id
		  AND status = :expected_status
		RETURNING *
		""")
                .bind("status", storageObject.getStorageObjectStatus().name())
                .bind("storage_id", storageObject.getStorageId())
                .bind("expected_status", expectedStatus.name())
                .mapProperties(StorageObjectJpaEntity.class)
                .one()
                .switchIfEmpty(
                        Mono.error(
                                new IllegalStateTransitionException(
                                        "Cannot transition object " + storageObject.getStorageId()
                                                + ": expected status " + expectedStatus
                                                + " in database, concurrent modification"
                                                + " detected")))
                .map(this.storageMapper::toDomain);
    }

    @Override
    public Flux<StorageObject> findPendingCreatedBefore(Instant cutoff) {
        return this.databaseClient
                .sql(
                        """
		SELECT * FROM store_objects
		WHERE status = 'PENDING'
		  AND created_at < :cutoff
		""")
                .bind("cutoff", cutoff)
                .mapProperties(StorageObjectJpaEntity.class)
                .all()
                .map(this.storageMapper::toDomain);
    }

    @Override
    public Flux<StorageObject> findRecentByOwner(String ownerUsername, int limit) {
        return this.databaseClient
                .sql(
                        """
		SELECT * FROM store_objects
		WHERE owner_username = :owner_username
		ORDER BY created_at DESC
		LIMIT :limit
		""")
                .bind("owner_username", ownerUsername)
                .bind("limit", limit)
                .mapProperties(StorageObjectJpaEntity.class)
                .all()
                .map(this.storageMapper::toDomain);
    }

    @Override
    public Mono<Void> touchLastSeen(Long storageId, Instant seenAt) {
        return this.databaseClient
                .sql(
                        """
		UPDATE store_objects
		SET last_modified_at = :seenAt
		WHERE storage_id = :storageId
		""")
                .bind("storageId", storageId)
                .bind("seenAt", seenAt)
                .then();
    }
}
