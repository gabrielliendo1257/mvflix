package com.gcorp.service.app.mvflix_movies.catalog.infrastructure.client.storage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gcorp.service.app.mvflix_movies.catalog.application.port.ManagedObjectDeletionInconsistentException;
import com.gcorp.service.app.mvflix_movies.catalog.application.port.ManagedObjectDeletionUnavailableException;
import com.gcorp.service.app.mvflix_movies.catalog.application.port.ManagedObjectReference;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Contrato del adapter M2M contra el endpoint de Storage. Sin contexto Spring:
 * un HttpServer simulado verifica URI, body, Bearer y la traducción de errores
 * HTTP a errores de aplicación.
 */
class StorageManagedObjectDeletionAdapterContractTest {

    private static final String PATH = "/api/v1/movie/storage/objects/42/deletion";

    private final ManagedObjectDeletionTokenProvider tokenProvider =
            mock(ManagedObjectDeletionTokenProvider.class);
    private final ObjectMapper mapper = new ObjectMapper();

    private HttpServer server;
    private int port;

    @BeforeEach
    void setUp() throws IOException {
        this.server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        this.port = this.server.getAddress().getPort();
        when(this.tokenProvider.token()).thenReturn(Mono.just("test-token"));
    }

    @AfterEach
    void tearDown() {
        this.server.stop(0);
    }

    private StorageManagedObjectDeletionAdapter adapter() {
        return new StorageManagedObjectDeletionAdapter(
                WebClient.builder(), this.tokenProvider, "http://127.0.0.1:" + this.port);
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
    void postsExpectedBodyAndBearerToDeletionEndpoint() throws IOException {
        AtomicReference<String> body = new AtomicReference<>();
        AtomicReference<String> authHeader = new AtomicReference<>();
        this.server.createContext(PATH, exchange -> {
            body.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            authHeader.set(exchange.getRequestHeaders().getFirst("Authorization"));
            respond(exchange, 204, "");
        });
        this.server.start();

        StepVerifier.create(this.adapter().delete(
                        new ManagedObjectReference(42L, "Admin", "Admin/videos/abc.mp4")))
                .verifyComplete();

        var json = this.mapper.readTree(body.get());
        assertThat(json.get("expectedOwner").asText()).isEqualTo("Admin");
        assertThat(json.get("expectedObjectKey").asText()).isEqualTo("Admin/videos/abc.mp4");
        assertThat(authHeader.get()).isEqualTo("Bearer test-token");
    }

    @Test
    void storage404IsIdempotentNoOp() throws IOException {
        this.server.createContext(PATH,
                exchange -> respond(exchange, 404, "{\"error\":\"STORAGE_OBJECT_NOT_AVAILABLE\"}"));
        this.server.start();

        StepVerifier.create(this.adapter().delete(
                        new ManagedObjectReference(42L, "Admin", "k")))
                .verifyComplete();
    }

    @Test
    void storage409MismatchIsInconsistent() throws IOException {
        this.server.createContext(PATH,
                exchange -> respond(exchange, 409, "{\"error\":\"OBJECT_MISMATCH\"}"));
        this.server.start();

        StepVerifier.create(this.adapter().delete(
                        new ManagedObjectReference(42L, "Admin", "k")))
                .expectError(ManagedObjectDeletionInconsistentException.class)
                .verify();
    }

    @Test
    void storage403ScopeIsInconsistent() throws IOException {
        this.server.createContext(PATH,
                exchange -> respond(exchange, 403, "{\"error\":\"FORBIDDEN\"}"));
        this.server.start();

        StepVerifier.create(this.adapter().delete(
                        new ManagedObjectReference(42L, "Admin", "k")))
                .expectError(ManagedObjectDeletionInconsistentException.class)
                .verify();
    }

    @Test
    void storage500IsUnavailable() throws IOException {
        this.server.createContext(PATH,
                exchange -> respond(exchange, 500, "{\"error\":\"INTERNAL_SERVER_ERROR\"}"));
        this.server.start();

        StepVerifier.create(this.adapter().delete(
                        new ManagedObjectReference(42L, "Admin", "k")))
                .expectError(ManagedObjectDeletionUnavailableException.class)
                .verify();
    }

    @Test
    void storageUnreachableIsUnavailable() {
        // Puerto 1 sin listener: conexión rechazada de forma determinista.
        var adapter = new StorageManagedObjectDeletionAdapter(
                WebClient.builder(), this.tokenProvider, "http://127.0.0.1:1");

        StepVerifier.create(adapter.delete(
                        new ManagedObjectReference(42L, "Admin", "k")))
                .expectError(ManagedObjectDeletionUnavailableException.class)
                .verify();
    }
}
