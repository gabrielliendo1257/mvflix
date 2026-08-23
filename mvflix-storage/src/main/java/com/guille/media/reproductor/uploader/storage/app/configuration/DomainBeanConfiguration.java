package com.guille.media.reproductor.uploader.storage.app.configuration;

import com.guille.media.reproductor.uploader.storage.domain.models.StorageKeyGenerator;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Registra como beans las colaboraciones de dominio puras (sin anotaciones de
 * framework) que la capa de aplicación necesita inyectar.
 */
@Configuration
public class DomainBeanConfiguration {

  @Bean
  public StorageKeyGenerator storageKeyGenerator() {
    return new StorageKeyGenerator();
  }
}
