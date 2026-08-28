package com.gcorp.mvflix.e2e;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.sql.Connection;
import java.sql.DriverManager;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jose4j.jwa.AlgorithmConstraints;
import org.jose4j.jws.AlgorithmIdentifiers;
import org.jose4j.jws.JsonWebSignature;
import org.jose4j.jwk.JsonWebKey;
import org.jose4j.jwk.RsaJsonWebKey;
import org.jose4j.jwt.JwtClaims;
import org.jose4j.lang.JoseException;
import org.junit.jupiter.api.Test;

class ManagedDeletionE2ETest {
  private static final String MOVIES = env("MOVIES_URL", "http://localhost:14040");
  private static final String STORAGE = env("STORAGE_URL", "http://localhost:16060");
  private static final ObjectMapper JSON = new ObjectMapper();
  private static final HttpClient HTTP = HttpClient.newHttpClient();
  private static final String USER = "e2e-" + UUID.randomUUID();

  @Test
  void deletesManagedObjectAcrossKafkaAndServices() throws Exception {
    String token = token(USER, "storage.write");
    post(STORAGE + "/api/v1/movie/storage/users/" + USER + "/provision", token,
        "{\"quota_bytes\":1048576}", 200);

    JsonNode upload = JSON.readTree(post(STORAGE + "/api/v1/movie/storage/upload", token,
        "{\"filename\":\"e2e.bin\",\"file_size\":4,\"mime_type\":\"application/octet-stream\"}", 200));
    String uploadUrl = upload.get("uploadUrl").asText().replace("http://minio:9000", "http://localhost:19000");
    put(uploadUrl, new byte[] {1, 2, 3, 4});
    long uploadId = upload.get("uploadId").asLong();
    post(STORAGE + "/api/v1/movie/storage/upload/" + uploadId + "/complete", token, null, 200, 202);

    long movieId = JSON.readTree(post(MOVIES + "/api/v1/movies", token,
        "{\"title\":\"E2E managed deletion\",\"kind\":\"MOVIE\"}", 200)).get("id").asLong();
    post(MOVIES + "/api/v1/movies/" + movieId + "/complete", token,
        "{\"object_id\":" + uploadId + ",\"object_key\":\"" + upload.get("storageKey").asText() + "\"}", 200);

    assertEquals(204, delete(MOVIES + "/api/v1/movies/" + movieId, token));
    assertEquals(204, delete(MOVIES + "/api/v1/movies/" + movieId, token));
    await().atMost(Duration.ofSeconds(90)).pollInterval(Duration.ofSeconds(2)).until(() -> {
      HttpResponse<String> response = get(MOVIES + "/api/v1/movies/" + movieId, token);
      return response.statusCode() == 404 && countStorageRows("managed_media_deletion_inbox") == 1
          && countStorageRows("store_objects") == 0;
    });
    assertTrue(true);
  }

  private static String token(String subject, String scope) throws Exception {
    String jwkJson = JSON.readTree(Files.readString(Path.of("../oidc-stub/jwks/jwks.json"))).get("keys").get(0).toString();
    RsaJsonWebKey key = (RsaJsonWebKey) JsonWebKey.Factory.newJwk(jwkJson);
    key.setPrivateKey(privateKey(Files.readString(Path.of("../oidc-stub/test-private-key.pem"))));
    JwtClaims claims = new JwtClaims();
    claims.setSubject(subject); claims.setClaim("scope", scope); claims.setIssuer("http://jwks-stub:8080");
    claims.setExpirationTimeMinutesInTheFuture(5); claims.setGeneratedJwtId();
    JsonWebSignature jws = new JsonWebSignature();
    jws.setAlgorithmHeaderValue(AlgorithmIdentifiers.RSA_USING_SHA256); jws.setKeyIdHeaderValue("mvflix-e2e-2026-01");
    jws.setKey(key.getPrivateKey()); jws.setPayload(claims.toJson());
    jws.setAlgorithmConstraints(new AlgorithmConstraints(AlgorithmConstraints.ConstraintType.PERMIT, AlgorithmIdentifiers.RSA_USING_SHA256));
    return jws.getCompactSerialization();
  }

  private static PrivateKey privateKey(String pem) throws Exception {
    String encoded = pem.replace("-----BEGIN PRIVATE KEY-----", "").replace("-----END PRIVATE KEY-----", "").replaceAll("\\s", "");
    return KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(java.util.Base64.getDecoder().decode(encoded)));
  }

  private static String post(String url, String token, String body, int... expected) throws Exception {
    HttpRequest.Builder b = HttpRequest.newBuilder(URI.create(url)).header("Authorization", "Bearer " + token).header("Content-Type", "application/json");
    HttpResponse<String> r = HTTP.send(b.POST(HttpRequest.BodyPublishers.ofString(body == null ? "" : body)).build(), HttpResponse.BodyHandlers.ofString());
    expect(r, expected); return r.body();
  }
  private static void put(String url, byte[] body) throws Exception { HttpResponse<Void> r = HTTP.send(HttpRequest.newBuilder(URI.create(url)).PUT(HttpRequest.BodyPublishers.ofByteArray(body)).build(), HttpResponse.BodyHandlers.discarding()); assertTrue(r.statusCode() < 300, r.toString()); }
  private static int delete(String url, String token) throws Exception { HttpResponse<String> r = HTTP.send(HttpRequest.newBuilder(URI.create(url)).header("Authorization", "Bearer " + token).DELETE().build(), HttpResponse.BodyHandlers.ofString()); expect(r, 204); return r.statusCode(); }
  private static HttpResponse<String> get(String url, String token) throws Exception { return HTTP.send(HttpRequest.newBuilder(URI.create(url)).header("Authorization", "Bearer " + token).GET().build(), HttpResponse.BodyHandlers.ofString()); }
  private static void expect(HttpResponse<?> r, int... expected) { for (int code : expected) if (r.statusCode() == code) return; throw new AssertionError("Expected " + java.util.Arrays.toString(expected) + ", got " + r.statusCode() + ": " + r.body()); }
  private static long countStorageRows(String table) { try (Connection c = DriverManager.getConnection("jdbc:postgresql://localhost:15432/mvflix_uploads_db", "db_ro", "12345678"); var s = c.createStatement(); var rs = s.executeQuery("select count(*) from " + table)) { rs.next(); return rs.getLong(1); } catch (Exception e) { return -1; } }
  private static String env(String name, String fallback) { return System.getenv().getOrDefault(name, fallback); }
}
