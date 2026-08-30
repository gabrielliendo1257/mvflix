package com.guille.media.reproductor.uploader.storage.managedstorage.infrastructure.persistence;

import com.guille.media.reproductor.uploader.storage.managedstorage.domain.model.StorageObject;
import com.guille.media.reproductor.uploader.storage.managedstorage.domain.model.StorageKey;
import com.guille.media.reproductor.uploader.storage.managedstorage.domain.model.StorageMetadata;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface StorageMapper {

    @Mapping(target = "objectKey", source = "storageKey.key")
    @Mapping(target = "idempotencyKey", source = "idempotencyKey")
    @Mapping(target = "contentType", source = "metadata.contentType")
    @Mapping(target = "contentLength", source = "metadata.contentLength")
    @Mapping(target = "checksum", source = "metadata.checksum")
    @Mapping(target = "lastModifiedAt", source = "metadata.lastModifiedAt")
    @Mapping(target = "createdAt", source = "createdAt")
    @Mapping(target = "status", source = "storageObjectStatus")
    StorageObjectJpaEntity toEntity(StorageObject storageObject);

    default StorageObject toDomain(StorageObjectJpaEntity entity) {
        return new StorageObject(entity.getOwnerUsername(), entity.getIdempotencyKey(),
            new StorageKey(entity.getObjectKey()),
            new StorageMetadata(entity.getContentType(), entity.getContentLength(),
                entity.getChecksum(), entity.getLastModifiedAt()), entity.getCreatedAt(),
            entity.getStorageId(), StorageObject.StorageSessionStatus.valueOf(entity.getStatus()));
    }
}
