package com.guille.media.reproductor.uploader.storage.infrastructure.database.storage;

import com.guille.media.reproductor.uploader.storage.domain.models.StoreObject;
import com.guille.media.reproductor.uploader.storage.domain.vos.StorageKey;
import com.guille.media.reproductor.uploader.storage.domain.vos.StorageMetadata;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface StorageMapper {

    @Mapping(target = "objectKey", source = "storageKey.key")
    @Mapping(target = "contentType", source = "metadata.contentType")
    @Mapping(target = "contentLength", source = "metadata.contentLength")
    @Mapping(target = "checksum", source = "metadata.checksum")
    @Mapping(target = "lastModifiedAt", source = "metadata.lastModifiedAt")
    @Mapping(target = "status", source = "storageObjectStatus")
    StoreObjectJpaEntity toEntity(StoreObject storageObject);

    @Mapping(target = "storageKey", expression = "java(new StorageKey(entity.getObjectKey()))")
    @Mapping(target = "metadata", expression = "java(new StorageMetadata(entity.getContentType(), entity.getContentLength(), entity.getChecksum(), entity.getLastModifiedAt()))")
    @Mapping(target = "storageSessionStatus", source = "status")
    StoreObject toDomain(StoreObjectJpaEntity entity);
}
