package com.guille.media.reproductor.uploader.storage.infrastructure.database.user;

import com.guille.media.reproductor.uploader.storage.domain.models.StorageQuota;
import com.guille.media.reproductor.uploader.storage.domain.models.StorageUsage;
import com.guille.media.reproductor.uploader.storage.domain.models.UserStorage;
import com.guille.media.reproductor.uploader.storage.domain.vos.BucketName;

import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface UserStorageMapper {

	UserStorage toDomain(UserStorageEntity entity);

	UserStorageEntity toEntity(UserStorage user);

	default BucketName toBucketName(String value) {
		return value == null ? null : BucketName.of(value);
	}

	default String toBucketName(BucketName value) {
		return value == null ? null : value.bucketName();
	}

	default StorageQuota toStorageQuota(Long value) {
		return value == null ? null : new StorageQuota(value);
	}

	default Long toStorageQuota(StorageQuota value) {
		return value == null ? null : value.getUserBytesQuota();
	}

	default StorageUsage toStorageUsage(Long value) {
		return value == null ? null : new StorageUsage(value);
	}

	default Long toStorageUsage(StorageUsage value) {
		return value == null ? null : value.getCurrentBytesUsage();
	}
}
