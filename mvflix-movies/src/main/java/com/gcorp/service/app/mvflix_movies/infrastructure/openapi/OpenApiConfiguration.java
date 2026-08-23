package com.gcorp.service.app.mvflix_movies.infrastructure.openapi;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfiguration {

    @Bean
    OpenAPI moviesOpenApi() {
        return new OpenAPI().info(new Info()
                .title("MVFLIX Movies API")
                .version("v1")
                .description(
                    """
                    Catálogo multimedia: drafts identificados (Add Media), ciclo
                    DRAFT→READY, visibilidad y compartición, enriquecimiento TMDB
                    e ingesta desde bibliotecas.
                    """));
    }
}
