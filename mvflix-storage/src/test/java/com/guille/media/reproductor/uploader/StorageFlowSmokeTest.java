package com.guille.media.reproductor.uploader;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.testcontainers.containers.MinIOContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;

/**
 * Smoke test del flujo completo sin levantar ningún tier aliado: Postgres y
 * MinIO corren en Testcontainers y el perfil {@code sandbox} sustituye al
 * authorization-service.
 *
 * <p>Valida: provision → crear sesión de upload → PUT real vía presigned URL
 * → complete → streaming → cuota → borrado → cuota liberada.
 */
@Testcontainers
@AutoConfigureWebTestClient
@ActiveProfiles("sandbox")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class StorageFlowSmokeTest {

  private static final String BASE = "/api/v1/movie/storage";
  private static final String USERNAME = "pepe";
  private static final long QUOTA_BYTES = 10L * 1024 * 1024;
  private static final long OBJECT_SIZE = 1024;

  @Container
  static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

  @Container
  static final MinIOContainer MINIO =
      new MinIOContainer("minio/minio:latest")
          .withUserName("minioadmin")
          .withPassword("minioadmin");

  @DynamicPropertySource
  static void properties(DynamicPropertyRegistry registry) {
    registry.add(
        "spring.r2dbc.url",
        () ->
            "r2dbc:postgresql://"
                + POSTGRES.getHost()
                + ":"
                + POSTGRES.getMappedPort(5432)
                + "/"
                + POSTGRES.getDatabaseName());
    registry.add("spring.r2dbc.username", POSTGRES::getUsername);
    registry.add("spring.r2dbc.password", POSTGRES::getPassword);
    registry.add(
        "spring.datasource.url",
        () ->
            "jdbc:postgresql://"
                + POSTGRES.getHost()
                + ":"
                + POSTGRES.getMappedPort(5432)
                + "/"
                + POSTGRES.getDatabaseName());
    registry.add("spring.datasource.username", POSTGRES::getUsername);
    registry.add("spring.datasource.password", POSTGRES::getPassword);
    registry.add("minio.url", MINIO::getS3URL);
    registry.add("minio.access-key", MINIO::getUserName);
    registry.add("minio.secret-key", MINIO::getPassword);
  }

  @Autowired WebTestClient client;

  @Autowired ObjectMapper objectMapper;

  @Test
  void storageLifecycle_provisionUploadCompleteStreamQuotaDelete() throws Exception {
    provisionUser();
    quota(0);

    JsonNode session = createUploadSession();
    String uploadId = session.path("uploadId").asText();
    performPut(session.path("uploadUrl").asText());

    client
        .post()
        .uri(BASE + "/upload/{uploadId}/complete", uploadId)
        .exchange()
        .expectStatus()
        .isOk();

    quota(OBJECT_SIZE);

    client
        .post()
        .uri(BASE + "/streaming")
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue("{\"objectId\":\"" + uploadId + "\"}")
        .exchange()
        .expectStatus()
        .isOk()
        .expectBody()
        .jsonPath("$.streamingUrl")
        .isNotEmpty();

    client
        .delete()
        .uri(BASE + "/{storageId}", uploadId)
        .exchange()
        .expectStatus()
        .isNoContent();

    quota(0);
  }

  private void provisionUser() {
    client
        .post()
        .uri(BASE + "/users/{username}/provision", USERNAME)
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue("{\"quota_bytes\":" + QUOTA_BYTES + "}")
        .exchange()
        .expectStatus()
        .isOk();
  }

  private void quota(long expectedUsedBytes) {
    client
        .get()
        .uri(BASE + "/quota")
        .exchange()
        .expectStatus()
        .isOk()
        .expectBody()
        .jsonPath("$.ownerUsername")
        .isEqualTo(USERNAME)
        .jsonPath("$.usedBytes")
        .isEqualTo(expectedUsedBytes)
        .jsonPath("$.quotaBytes")
        .isEqualTo(QUOTA_BYTES);
  }

  private JsonNode createUploadSession() throws Exception {
    String response =
        client
            .post()
            .uri(BASE + "/upload")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(
                "{\"filename\":\"movie.mp4\",\"file_size\":"
                    + OBJECT_SIZE
                    + ",\"mime_type\":\"video/mp4\"}")
            .exchange()
            .expectStatus()
            .isOk()
            .expectBody(String.class)
            .returnResult()
            .getResponseBody();

    JsonNode body = this.objectMapper.readTree(response);
    assertThat(body.path("uploadId").asText()).isNotBlank();
    assertThat(body.path("uploadUrl").asText()).isNotBlank();
    return body;
  }

  private void performPut(String uploadUrl) throws Exception {
    HttpURLConnection connection =
        (HttpURLConnection) URI.create(uploadUrl).toURL().openConnection();
    connection.setRequestMethod("PUT");
    connection.setRequestProperty("Content-Type", "video/mp4");
    connection.setDoOutput(true);

    byte[] payload = new byte[(int) OBJECT_SIZE];
    try (OutputStream output = connection.getOutputStream()) {
      output.write(payload);
    }

    int status = connection.getResponseCode();
    connection.disconnect();

    assertThat(status).isEqualTo(HttpStatus.OK.value());
  }
}