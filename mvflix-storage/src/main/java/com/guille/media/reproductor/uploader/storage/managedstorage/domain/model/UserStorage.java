package com.guille.media.reproductor.uploader.storage.managedstorage.domain.model;

import com.guille.media.reproductor.uploader.storage.managedstorage.domain.model.BucketName;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;

/**
 * Cuenta de almacenamiento de un usuario: bucket dedicado, cuota y uso. La
 * invariante cuota/uso NO se aplica aquí: la autoritativa es el UPDATE
 * condicional atómico de {@code UserStorageRepository.consumeStorage}, que es
 * quien puede garantizarla bajo concurrencia (ver ADR 0001).
 */
@Getter
@ToString
@AllArgsConstructor
public class UserStorage {
  private final Long id;
  private final BucketName bucketName;
  private final String ownerUsername;
  private StorageQuota storageQuota;
  private StorageUsage storageUsage;
}
