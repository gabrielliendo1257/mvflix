package com.guille.media.reproductor.uploader.storage.infrastructure.database.user;

import com.guille.media.reproductor.uploader.storage.domain.models.UserStorage;
import com.guille.media.reproductor.uploader.storage.domain.ports.UserStorageRepository;

import com.guille.media.reproductor.uploader.storage.infrastructure.database.storage.StoreObjectJpaEntity;
import lombok.RequiredArgsConstructor;

import org.springframework.r2dbc.core.DatabaseClient;

import reactor.core.publisher.Mono;

@RequiredArgsConstructor
public class SpringDataUserStorageRepository implements UserStorageRepository {

    private final DatabaseClient databaseClient;
    private final UserStorageMapper userStorageMapper;

    @Override
    public Mono<UserStorage> save(UserStorage user) {
        UserStorageEntity entity = this.userStorageMapper.toEntity(user);
        return this.databaseClient
                .sql(
                        """
	INSERT INTO user_storage (
		user_storage_id,
		owner_username,
		bucket_name,
		storage_quota,
		storage_usage
		createdAt
	)
	VALUES (
		:userStorageId,
		:ownerUsername,
		:bucketName,
		:storageQuota,
		:storageUsage,
		:createdAt
	)
	RETURNING *
	""")
                .bindProperties(entity)
                .mapProperties(UserStorageEntity.class)
                .one()
                .map(this.userStorageMapper::toDomain);
    }

    @Override
    public Mono<UserStorage> findById(Long userStorageId) {
		return this.databaseClient
			.sql(
				"""
		SELECT * FROM user_storage WHERE user_storage_id = :user_storage_id
		""")
			.bind("user_storage_id", userStorageId)
			.mapProperties(UserStorageEntity.class)
			.one()
			.map(this.userStorageMapper::toDomain);
    }

    @Override
    public Mono<UserStorage> findByOwnerUsername(String ownerUsername) {
		return this.databaseClient
			.sql(
				"""
		SELECT * FROM users WHERE owner_username = :owner_username
		""")
			.bind("owner_username", ownerUsername)
			.mapProperties(UserStorageEntity.class)
			.one()
			.map(this.userStorageMapper::toDomain);
    }
}
