package com.guille.media.reproductor.uploader.storage.infrastructure.database.user;

import com.guille.media.reproductor.uploader.storage.domain.models.UserStorage;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface UserStorageMapper {

	UserStorage toDomain(UserStorageEntity entity);
	UserStorageEntity toEntity(UserStorage entity);
}
