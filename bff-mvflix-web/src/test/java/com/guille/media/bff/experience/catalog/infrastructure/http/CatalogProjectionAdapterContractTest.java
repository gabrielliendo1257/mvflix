package com.guille.media.bff.experience.catalog.infrastructure.http;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.guille.media.bff.experience.catalog.application.CatalogPage;
import com.guille.media.bff.experience.catalog.infrastructure.http.CatalogProjectionAdapter.DownstreamPage;

import org.junit.jupiter.api.Test;
import org.springframework.web.util.DefaultUriBuilderFactory;

/**
 * Protege al CONSUMIDOR del contrato de movies: URI y query params que se
 * envían, deserialización tolerante a campos desconocidos, conservación de
 * assetPresent (el campo que ya se perdió una vez) y mapping downstream →
 * application con capabilities derivadas.
 */
class CatalogProjectionAdapterContractTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void uriContainsPathAndOnlyProvidedQueryParameters() {
        var factory = new DefaultUriBuilderFactory("http://movies");
        var uri = CatalogProjectionAdapter.catalogUri(
                factory.builder(), 1, 10, "ali", "READY", "year", "desc").build().toString();

        assertThat(uri).isEqualTo(
                "http://movies/api/v1/movies/catalog?page=1&size=10&q=ali&status=READY&sort=year&dir=desc");
    }

    @Test
    void blankOptionalParametersAreOmittedFromUri() {
        var factory = new DefaultUriBuilderFactory("http://movies");
        var uri = CatalogProjectionAdapter.catalogUri(
                factory.builder(), 0, 25, null, null, null, null).build().toString();

        assertThat(uri).isEqualTo("http://movies/api/v1/movies/catalog?page=0&size=25");
    }

    private static final String MOVIES_JSON = """
            {
              "summary": {"total": 128, "ready": 121, "needsAttention": 7},
              "items": [{
                 "key": {"type": "MEDIA", "id": 42},
                 "mediaId": 42,
                 "assetId": 17,
                 "assetPresent": false,
                 "title": "Coraline",
                 "posterUrl": "/c.jpg",
                 "year": 2009,
                 "duration": "1h 40m",
                 "kind": "MOVIE",
                 "status": "READY",
                 "displayStatus": "MISSING",
                 "source": "LOCAL",
                 "visibility": "PRIVATE",
                 "sharedWithCount": 3,
                 "providerStatus": "LINKED",
                 "futureField": "tolerado"
              }],
              "page": 0, "size": 25, "total": 128, "totalPages": 6,
              "futureBlock": {"x": 1}
            }
            """;

    @Test
    void parsesMoviesJsonPreservingAssetPresentAndToleratingUnknownFields() throws Exception {
        DownstreamPage downstream = this.mapper.readValue(MOVIES_JSON, DownstreamPage.class);
        CatalogPage page = CatalogProjectionAdapter.toApplication(downstream);

        assertThat(page.total()).isEqualTo(128);
        assertThat(page.totalPages()).isEqualTo(6);
        var item = page.items().get(0);
        // El campo crítico sobrevive al parseo: sin él play=false siempre.
        assertThat(item.assetPresent()).isFalse();
        assertThat(item.key().type()).isEqualTo("MEDIA");
        assertThat(item.getCapabilities().play()).isFalse();
        assertThat(item.getCapabilities().delete()).isFalse();
    }

    @Test
    void presentLocalAssetMapsToPlayableCapability() throws Exception {
        String json = MOVIES_JSON
                .replace("\"assetPresent\": false", "\"assetPresent\": true")
                .replace("\"displayStatus\": \"MISSING\"", "\"displayStatus\": \"READY\"");

        CatalogPage page = CatalogProjectionAdapter.toApplication(
                this.mapper.readValue(json, DownstreamPage.class));

        assertThat(page.items().get(0).getCapabilities().play()).isTrue();
    }
}
