package com.guille.media.bff.infrastructure.http;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;

import reactor.test.StepVerifier;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Contrato del adapter HTTP REAL para el acceso: verifica que el PUT que sale
 * hacia movies serializa {@code sharedWith} en camelCase (el nombre que movies
 * deserializa). Un test de ObjectMapper sobre el record no cubre el intercambio
 * real del WebClient.
 */
class MoviesWebClientAdapterAccessHttpContractTest {

    private final ObjectMapper mapper = new ObjectMapper();
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

    private void respond(HttpExchange exchange, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream out = exchange.getResponseBody()) {
            out.write(bytes);
        }
    }

    @Test
    void sendsSharedWithInCamelCaseThroughRealHttp() throws IOException {
        AtomicReference<String> capturedBody = new AtomicReference<>();
        this.server.createContext("/api/v1/movies/42/access", exchange -> {
            capturedBody.set(new String(exchange.getRequestBody().readAllBytes(),
                    StandardCharsets.UTF_8));
            respond(exchange, 200, "{\"id\":42,\"title\":\"Coraline\"}");
        });
        this.server.start();

        var adapter = new MoviesWebClientAdapter(
                WebClient.builder().baseUrl("http://127.0.0.1:" + this.port).build());

        StepVerifier.create(adapter.updateMovieAccess(42L, "SHARED", List.of("Maria", "Pedro")))
                .assertNext(movie -> assertThat(movie.title()).isEqualTo("Coraline"))
                .verifyComplete();

        var json = this.mapper.readTree(capturedBody.get());
        assertThat(json.get("visibility").asText()).isEqualTo("SHARED");
        assertThat(json.has("sharedWith")).isTrue();
        assertThat(json.has("shared_with")).isFalse();
        assertThat(json.get("sharedWith").get(0).asText()).isEqualTo("Maria");
        assertThat(json.get("sharedWith").get(1).asText()).isEqualTo("Pedro");
    }
}
