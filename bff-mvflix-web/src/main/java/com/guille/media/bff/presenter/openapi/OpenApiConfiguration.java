package com.guille.media.bff.presenter.openapi;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfiguration {

    @Bean
    OpenAPI bffOpenApi() {
        return new OpenAPI().info(new Info()
                .title("MVFLIX Web BFF API")
                .version("v1")
                .description(
                    """
                    Experiencias del frontend web. Add Media encapsula la
                    coreografía Movies+Storage detrás de una sola intención;
                    el resto expone catálogo, bibliotecas y sesión.
                    """));
    }
}
