package com.guille.media.bff.experience.media.infrastructure.http;

import static org.assertj.core.api.Assertions.assertThat;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import com.guille.media.bff.experience.media.application.MediaDetail;
import com.guille.media.bff.experience.media.application.MediaDetailNotFoundException;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;

import reactor.test.StepVerifier;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

/**
 * Contrato del adapter HTTP REAL: el WebClient apunta a un HttpServer local y
 * verifica el intercambio completo (URI + deserialización snake→camel). Los
 * tests de ObjectMapper pinaban el record, pero no probaban que el adapter
 * parseara el JSON que movies produce de verdad.
 */
class MediaDetailProjectionAdapterHttpContractTest {

    private HttpServer server;
    private int port;

    @BeforeEach
    void setUp() throws IOException {
        this.server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        this.port = this.server.getAddress().getPort();
    }

    @AfterEach
    void tearDown() {
        this.server.stop(0);
    }

    private MediaDetailProjectionAdapter adapter() {
        return new MediaDetailProjectionAdapter(
                WebClient.builder().baseUrl("http://127.0.0.1:" + this.port).build());
    }

    private void respond(HttpExchange exchange, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream out = exchange.getResponseBody()) {
            out.write(bytes);
        }
    }

    @Test
    void parsesSnakeCasePosterAndTmdbIdThroughRealHttp() throws IOException {
        this.server.createContext("/api/v1/movies/42", exchange ->
                respond(exchange, 200, """
                        {"id":42,"title":"Coraline","status":"READY","kind":"MOVIE",
                         "visibility":"PRIVATE","poster_path":"/c.jpg","tmdb_id":57892,
                         "object_id":77,"enrichment_status":"ENRICHED",
                         "duration":"1h 40m","year":2009}
                        """));
        // MANAGED: sin asset vinculado, by-movie responde 404.
        this.server.createContext("/api/v1/movies/media-assets/by-movie/42", exchange ->
                respond(exchange, 404, "{}"));
        this.server.start();

        StepVerifier.create(this.adapter().detail(42L))
                .assertNext(detail -> {
                    assertThat(detail.overview().posterUrl()).isEqualTo("/c.jpg");
                    assertThat(detail.provider().status()).isEqualTo("LINKED");
                    assertThat(detail.provider().providerId()).isEqualTo(57892L);
                    assertThat(detail.access().source()).isEqualTo("MANAGED");
                })
                .verifyComplete();
    }

    @Test
    void localMediaZipsTheLinkedAssetThroughRealHttp() throws IOException {
        this.server.createContext("/api/v1/movies/9", exchange ->
                respond(exchange, 200, """
                        {"id":9,"title":"Alien","status":"READY","kind":"MOVIE",
                         "visibility":"PRIVATE","duration":"1h 57m","year":1979}
                        """));
        this.server.createContext("/api/v1/movies/media-assets/by-movie/9", exchange ->
                respond(exchange, 200, """
                        {"id":17,"libraryId":7,"relativePath":"Movies/alien.mp4",
                         "size":10,"mimeType":"video/mp4","status":"IDENTIFIED",
                         "present":true,"movieId":9}
                        """));
        this.server.start();

        StepVerifier.create(this.adapter().detail(9L))
                .assertNext(detail -> {
                    assertThat(detail.access().source()).isEqualTo("LOCAL");
                    assertThat(detail.access().assetId()).isEqualTo(17L);
                    assertThat(detail.access().assetPresent()).isTrue();
                    // Sin tmdb_id: providerStatus NONE.
                    assertThat(detail.provider().status()).isEqualTo("NONE");
                })
                .verifyComplete();
    }

    @Test
    void notFoundMovieSurfacesAsMediaDetailNotFound() throws IOException {
        this.server.createContext("/api/v1/movies/99", exchange ->
                respond(exchange, 404, "{\"error\":\"not found\"}"));
        this.server.start();

        StepVerifier.create(this.adapter().detail(99L))
                .expectError(MediaDetailNotFoundException.class)
                .verify();
    }
}
