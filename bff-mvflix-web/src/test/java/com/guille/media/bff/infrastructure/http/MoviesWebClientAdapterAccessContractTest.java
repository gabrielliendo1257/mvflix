package com.guille.media.bff.infrastructure.http;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.Test;

import java.util.List;

/**
 * Pin del cuerpo SALIENTE del endpoint de acceso: el BFF debe serializar
 * {@code sharedWith} (camelCase), el nombre que movies deserializa. Si alguien
 * reintroduce {@code shared_with} (snake), este test falla.
 */
class MoviesWebClientAdapterAccessContractTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void accessBodySerializesSharedWithInCamelCase() throws Exception {
        var json = this.mapper.writeValueAsString(
                new MoviesWebClientAdapter.AccessBody("SHARED", List.of("Maria", "Pedro")));
        var node = this.mapper.readTree(json);

        assertThat(node.has("sharedWith")).isTrue();
        assertThat(node.has("shared_with")).isFalse();
        assertThat(node.get("visibility").asText()).isEqualTo("SHARED");
        assertThat(node.get("sharedWith").get(0).asText()).isEqualTo("Maria");
        assertThat(node.get("sharedWith").get(1).asText()).isEqualTo("Pedro");
    }
}
