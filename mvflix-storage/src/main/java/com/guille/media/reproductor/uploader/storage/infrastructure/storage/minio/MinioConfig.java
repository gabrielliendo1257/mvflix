package com.guille.media.reproductor.uploader.storage.infrastructure.storage.minio;

import io.minio.MinioAsyncClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Cliente asíncrono de MinIO.
 *
 * <p>La aplicación es reactiva (WebFlux + R2DBC): el cliente clásico
 * ({@code MinioClient}) realiza I/O bloqueante sobre el event loop de
 * Netty, por lo que se usa {@code MinioAsyncClient}, cuyas operaciones
 * devuelven {@code CompletableFuture} y se integran con Reactor vía
 * {@code Mono.fromFuture}.
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
}