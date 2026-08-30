package com.gcorp.service.app.mvflix_movies.catalog.infrastructure.web.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.gcorp.service.app.mvflix_movies.catalog.domain.movie.CatalogItemVisibility;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.Test;

/**
 * Contrato al nivel del WIRE del endpoint de acceso. Fija que el campo es
 * {@code sharedWith} (camelCase), como el resto de DTOs de movies
 * ({@code CreateIdentifiedDraftRequest.sharedWith}, {@code usernames}).
 *
 * <p>El segundo caso documenta el bug que motiva este contrato: con el mapper
 * de Spring (fail-on-unknown deshabilitado), un {@code shared_with} snake es
 * SILENCIOSAMENTE ignorado y SHARED se guardaría sin lista de compartidos.
 */
class UpdateMovieAccessRequestWireTest {

    private final ObjectMapper mapper = new ObjectMapper()
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

    @Test
    void deserializesCamelCaseSharedWith() throws Exception {
        var request = this.mapper.readValue(
                "{\"visibility\":\"SHARED\",\"sharedWith\":[\"Maria\",\"Pedro\"]}",
                UpdateMovieAccessRequest.class);

        assertThat(request.visibility()).isEqualTo(CatalogItemVisibility.SHARED);
        assertThat(request.sharedWith()).containsExactly("Maria", "Pedro");
    }

    @Test
    void snakeCaseSharedWithIsSilentlyLost() throws Exception {
        // Lo que pasaría hoy si el BFF mandara shared_with: Jackson lo ignora
        // y sharedWith llega null -> SHARED sin compartidos.
        var request = this.mapper.readValue(
                "{\"visibility\":\"SHARED\",\"shared_with\":[\"Maria\"]}",
                UpdateMovieAccessRequest.class);

        assertThat(request.sharedWith()).isNull();
    }
}
