package com.gcorp.service.app.mvflix_movies.catalog.infrastructure.web.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.gcorp.service.app.mvflix_movies.catalog.application.CatalogItemView;
import com.gcorp.service.app.mvflix_movies.catalog.application.CatalogPageView;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.Test;

import java.util.List;

/**
 * Contrato al nivel del WIRE: lo que el BFF consume es el JSON producido por
 * Jackson, no el record de aplicación. assetPresent se perdió una vez por
 * existir solo en la vista; este test impide que vuelva a desaparecer sin
 * romper compilación.
 */
class CatalogPageResponseWireTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void itemJsonCarriesAssetPresentForLocalContent() throws Exception {
        var view = new CatalogPageView(
                new CatalogPageView.Summary(1, 1, 0),
                List.of(new CatalogItemView(
                        CatalogItemView.Key.media(42L), 42L, 17L, Boolean.TRUE,
                        "Coraline", "/c.jpg", 2009, "1h 40m", "MOVIE",
                        "READY", "READY", "LOCAL", "PRIVATE", 0, "LINKED")),
                0, 25, 1, 1);

        JsonNode json = mapper.readTree(
                mapper.writeValueAsString(CatalogPageResponse.from(view)));

        var item = json.get("items").get(0);
        assertThat(item.has("assetPresent")).isTrue();
        assertThat(item.get("assetPresent").asBoolean()).isTrue();
        assertThat(item.get("source").asText()).isEqualTo("LOCAL");
    }

    @Test
    void managedItemSerializesAssetPresentExplicitlyNull() throws Exception {
        var view = new CatalogPageView(
                new CatalogPageView.Summary(1, 1, 0),
                List.of(new CatalogItemView(
                        CatalogItemView.Key.media(7L), 7L, null, null,
                        "Beta", null, null, null, "MOVIE",
                        "READY", "READY", "MANAGED", "PRIVATE", 0, "NONE")),
                0, 25, 1, 1);

        JsonNode json = mapper.readTree(
                mapper.writeValueAsString(CatalogPageResponse.from(view)));

        // Explícito aunque null: el BFF distingue "sin aplicable" de "omitido".
        var item = json.get("items").get(0);
        assertThat(item.has("assetPresent")).isTrue();
        assertThat(item.get("assetPresent").isNull()).isTrue();
    }
}
