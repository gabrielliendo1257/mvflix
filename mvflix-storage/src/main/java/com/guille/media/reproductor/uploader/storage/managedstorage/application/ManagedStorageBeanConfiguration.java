package com.guille.media.reproductor.uploader.storage.managedstorage.application;

import com.guille.media.reproductor.uploader.storage.managedstorage.domain.model.StorageKeyGenerator;
import com.guille.media.reproductor.uploader.storage.managedstorage.domain.policy.DefaultUploadPolicy;
import com.guille.media.reproductor.uploader.storage.managedstorage.domain.policy.UploadPolicy;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.ReactiveTransactionManager;
import org.springframework.transaction.reactive.TransactionalOperator;

/**
 * Registra como beans las colaboraciones de dominio puras (sin anotaciones de
 * framework) que la capa de aplicación necesita inyectar.
 */
@Configuration
public class ManagedStorageBeanConfiguration {

  @Bean
  public StorageKeyGenerator storageKeyGenerator() {
    return new StorageKeyGenerator();
  }

  @Bean
  public UploadPolicy uploadPolicy() {
    return new DefaultUploadPolicy();
  }

  @Bean
  public TransactionalOperator transactionalOperator(
      ReactiveTransactionManager transactionManager) {
    return TransactionalOperator.create(transactionManager);
  }
}
