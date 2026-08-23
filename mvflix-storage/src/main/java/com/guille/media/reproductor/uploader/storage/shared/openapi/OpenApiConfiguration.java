package com.guille.media.reproductor.uploader.storage.shared.openapi;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfiguration {

    @Bean
    OpenAPI storageOpenApi() {
        return new OpenAPI().info(new Info()
                .title("MVFLIX Storage API")
                .version("v1")
                .description(
                    """
                    Objetos gestionados (uploads con cuota, streaming presignado,
                    limpieza) y bibliotecas locales (registro, escaneo, serving).
                    Autenticación OAuth2 bearer emitida por authorization-service;
                    el webhook MinIO usa token compartido en /internal/minio/events.
                    """));
    }
}
