package com.guille.media.reproductor.uploader.storage.managedstorage.infrastructure.objectstore;

import io.minio.MinioAsyncClient;
import io.minio.MinioClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Clientes de MinIO.
 *
 * <p>La aplicación es reactiva (WebFlux + R2DBC): el cliente bloqueante
 * ({@code MinioClient}) no debe usarse en el event loop de Netty, por lo que
 * las operaciones del servicio se resuelven con {@code MinioAsyncClient},
 * cuyas llamadas devuelven {@code CompletableFuture} y se integran con
 * Reactor vía {@code Mono.fromFuture}.
 *
 * <p>El {@code MinioClient} clásico se manten para operaciones administrativas
 * síncronas (listar, mover, copiar, borrar) que nunca se invocan desde el
 * event loop.
 */
@Configuration
public class MinioConfig {

    @Value("${minio.url}")
    private String endpoint;

    @Value("${minio.access-key}")
    private String accessKey;

    @Value("${minio.secret-key}")
    private String secretKey;

    @Bean
    MinioAsyncClient minioAsyncClient() {
        return MinioAsyncClient.builder()
                .endpoint(this.endpoint)
                .credentials(this.accessKey, this.secretKey)
                .build();
    }

    @Bean
    MinioClient minioClient() {
        return MinioClient.builder()
                .endpoint(this.endpoint)
                .credentials(this.accessKey, this.secretKey)
                .build();
    }
}