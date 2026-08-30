package com.guille.media.reproductor.uploader.storage.managedstorage.infrastructure.persistence;

import com.guille.media.reproductor.uploader.storage.managedstorage.domain.model.StorageObject;
import com.guille.media.reproductor.uploader.storage.managedstorage.domain.model.StorageKey;
import com.guille.media.reproductor.uploader.storage.managedstorage.domain.model.StorageMetadata;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface StorageMapper {

    @Mapping(target = "objectKey", source = "storageKey.key")
    @Mapping(target = "contentType", source = "metadata.contentType")
    @Mapping(target = "contentLength", source = "metadata.contentLength")
    @Mapping(target = "checksum", source = "metadata.checksum")
    @Mapping(target = "lastModifiedAt", source = "metadata.lastModifiedAt")
    @Mapping(target = "createdAt", source = "createdAt")
    @Mapping(target = "status", source = "storageObjectStatus")
    StorageObjectJpaEntity toEntity(StorageObject storageObject);

    @Mapping(target = "storageKey", expression = "java(new StorageKey(entity.getObjectKey()))")
    @Mapping(target = "metadata", expression = "java(new StorageMetadata(entity.getContentType(), entity.getContentLength(), entity.getChecksum(), entity.getLastModifiedAt()))")
    @Mapping(target = "storageSessionStatus", source = "status")
    StorageObject toDomain(StorageObjectJpaEntity entity);
}
