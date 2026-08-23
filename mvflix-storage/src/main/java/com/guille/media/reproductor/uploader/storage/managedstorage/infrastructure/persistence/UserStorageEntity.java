package com.guille.media.reproductor.uploader.storage.managedstorage.infrastructure.persistence;

import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

@Data
@Table(name = "user_storage")
@Builder
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class UserStorageEntity {

	@Id private Long userStorageId;

	private String ownerUsername;

	private String bucketName;

	private Long storageQuota;

	private Long storageUsage;

	private Instant createdAt;
}
