package com.guille.media.reproductor.uploader.storage.managedstorage.infrastructure.persistence;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;

@Data
@Table(name = "store_objects")
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StorageObjectJpaEntity {

    @Id private Long storageId;

    private String ownerUsername;

    private String objectKey;

    private String status;

    private String contentType;

    private Long contentLength;

    private String checksum;

    private Instant createdAt;

    private Instant lastModifiedAt;
}
