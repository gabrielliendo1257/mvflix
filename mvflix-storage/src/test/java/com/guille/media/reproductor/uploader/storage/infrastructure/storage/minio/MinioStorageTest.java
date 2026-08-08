package com.guille.media.reproductor.uploader.storage.infrastructure.storage.minio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.guille.media.reproductor.uploader.storage.domain.exceptions.StorageException;
import com.guille.media.reproductor.uploader.storage.domain.vos.BucketName;
import com.guille.media.reproductor.uploader.storage.domain.vos.PermissionUrl;
import com.guille.media.reproductor.uploader.storage.domain.vos.PresignedUploadRequest;
import com.guille.media.reproductor.uploader.storage.domain.vos.StorageKey;
import com.guille.media.reproductor.uploader.storage.domain.vos.StorageLocation;
import com.guille.media.reproductor.uploader.storage.domain.vos.StorageMetadata;
import com.guille.media.reproductor.uploader.storage.domain.vos.StoredObjectSummary;
import io.minio.MinioAsyncClient;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import java.io.ByteArrayInputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.MinIOContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
class MinioStorageTest {

  @Container
  static final MinIOContainer MINIO_CONTAINER =
      new MinIOContainer("minio/minio:latest")
          .withUserName("minioadmin")
          .withPassword("minioadmin");

  private static MinioAsyncClient asyncClient;
  private static MinioClient syncClient;
  private static MinioStorage storage;

  @BeforeAll
  static void setUp() {
    asyncClient =
        MinioAsyncClient.builder()
            .endpoint(MINIO_CONTAINER.getS3URL())
            .credentials(MINIO_CONTAINER.getUserName(), MINIO_CONTAINER.getPassword())
            .region("us-east-1")
            .build();
    syncClient =
        MinioClient.builder()
            .endpoint(MINIO_CONTAINER.getS3URL())
            .credentials(MINIO_CONTAINER.getUserName(), MINIO_CONTAINER.getPassword())
            .region("us-east-1")
            .build();
    storage = new MinioStorage(asyncClient, syncClient);
  }

  private static StorageLocation location(BucketName bucket, String key) {
    return new StorageLocation(bucket, new StorageKey(key));
  }

  private static void upload(BucketName bucket, String key, String content) throws Exception {
    byte[] bytes = content.getBytes(StandardCharsets.UTF_8);
    syncClient.putObject(
        PutObjectArgs.builder()
            .bucket(bucket.bucketName())
            .object(key)
            .contentType("text/plain")
            .stream(new ByteArrayInputStream(bytes), bytes.length, -1)
            .build());
  }

  @Test
  void createBucket_makesBucketVisibleToBucketExists() {
    BucketName bucket = BucketName.of("test-create-bucket");

    storage.createBucket(bucket.bucketName()).block();

    assertThat(storage.bucketExists(bucket).block()).isTrue();
  }

  @Test
  void bucketExists_unknownBucket_returnsFalse() {
    assertThat(storage.bucketExists(BucketName.of("unknown-bucket")).block()).isFalse();
  }

  @Test
  void ensureBucket_isIdempotent() {
    BucketName bucket = BucketName.of("test-ensure-bucket");

    storage.ensureBucket(bucket).block();
    storage.ensureBucket(bucket).block();

    assertThat(storage.bucketExists(bucket).block()).isTrue();
  }

  @Test
  void ensureUserStorageLayout_materializesAllFolders() {
    BucketName bucket = BucketName.of("test-layout-bucket");
    storage.ensureBucket(bucket).block();
    String username = "pepe";

    storage.ensureUserStorageLayout(bucket, username).block();

    List<StoredObjectSummary> objects = storage.list(bucket, username + "/");
    assertThat(objects)
        .extracting(StoredObjectSummary::objectName)
        .containsExactlyInAnyOrder(
            username + "/images/",
            username + "/videos/",
            username + "/compressed/",
            username + "/executables/",
            username + "/private/");
  }

  @Test
  void objectExists_existingObject_returnsTrue_andReadsMetadata() throws Exception {
    BucketName bucket = BucketName.of("test-metadata-bucket");
    storage.createBucket(bucket.bucketName()).block();

    String content = "hello async minio";
    upload(bucket, "folder/video.mp4", content);

    StorageLocation location = location(bucket, "folder/video.mp4");

    assertThat(storage.objectExists(location).block()).isTrue();

    StorageMetadata metadata = storage.getMetadata(location).block();
    assertThat(metadata).isNotNull();
    assertThat(metadata.contentLength()).isEqualTo(content.getBytes(StandardCharsets.UTF_8).length);
    assertThat(metadata.contentType()).isEqualTo("text/plain");
    assertThat(metadata.lastModifiedAt()).isNotNull();
  }

  @Test
  void objectExists_missingObject_returnsFalse() {
    BucketName bucket = BucketName.of("test-missing-object-bucket");
    storage.createBucket(bucket.bucketName()).block();

    StorageLocation location = location(bucket, "does-not-exist.txt");

    assertThat(storage.objectExists(location).block()).isFalse();
  }

  @Test
  void objectExists_unknownBucket_returnsFalse() {
    StorageLocation location = location(BucketName.of("no-such-bucket"), "any-object");

    assertThat(storage.objectExists(location).block()).isFalse();
  }

  @Test
  void getMetadata_missingObject_failsWithStorageError() {
    BucketName bucket = BucketName.of("test-missing-meta-bucket");
    storage.createBucket(bucket.bucketName()).block();

    StorageLocation location = location(bucket, "does-not-exist.txt");

    assertThatThrownBy(() -> storage.getMetadata(location).block())
        .isInstanceOf(StorageException.class);
  }

  @Test
  void createUploadUrl_acceptsPresignedPut_andObjectBecomesVisible() throws Exception {
    BucketName bucket = BucketName.of("test-upload-url-bucket");
    storage.createBucket(bucket.bucketName()).block();

    StorageLocation location = location(bucket, "uploads/demo.txt");
    String content = "hello uploaded via presigned url";

    PermissionUrl url =
        storage
            .createUploadUrl(new PresignedUploadRequest(Duration.ofMinutes(5)), location)
            .block();

    assertThat(url.method()).isEqualTo("PUT");
    assertThat(url.presignedUrl()).startsWith(MINIO_CONTAINER.getS3URL());

    HttpRequest request =
        HttpRequest.newBuilder(URI.create(url.presignedUrl()))
            .PUT(HttpRequest.BodyPublishers.ofByteArray(content.getBytes(StandardCharsets.UTF_8)))
            .build();
    HttpResponse<String> response =
        HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());

    assertThat(response.statusCode()).isEqualTo(200);
    assertThat(storage.objectExists(location).block()).isTrue();
    assertThat(storage.getMetadata(location).block().contentLength())
        .isEqualTo(content.getBytes(StandardCharsets.UTF_8).length);
  }

  @Test
  void createStreamingUrl_returnsUploadedContent() throws Exception {
    BucketName bucket = BucketName.of("test-streaming-url-bucket");
    storage.createBucket(bucket.bucketName()).block();

    String content = "hello streaming content";
    upload(bucket, "media/movie.mp4", content);

    StorageLocation location = location(bucket, "media/movie.mp4");
    PermissionUrl url =
        storage
            .createStreamingUrl(new PresignedUploadRequest(Duration.ofMinutes(5)), location)
            .block();

    assertThat(url.method()).isEqualTo("GET");

    HttpRequest request = HttpRequest.newBuilder(URI.create(url.presignedUrl())).GET().build();
    HttpResponse<String> response =
        HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());

    assertThat(response.statusCode()).isEqualTo(200);
    assertThat(response.body()).isEqualTo(content);
  }

  @Test
  void copyMoveDeleteAndList_workAgainstRealBucket() throws Exception {
    BucketName bucket = BucketName.of("test-ops-bucket");
    storage.createBucket(bucket.bucketName()).block();

    upload(bucket, "folder/a.txt", "aaa");
    upload(bucket, "folder/b.txt", "bbb");
    upload(bucket, "other/c.txt", "ccc");

    StorageLocation source = location(bucket, "folder/a.txt");
    StorageLocation copyTarget = location(bucket, "folder/a-copy.txt");
    StorageLocation movedTarget = location(bucket, "other/a-moved.txt");

    storage.copy(source, copyTarget);
    assertThat(storage.objectExists(copyTarget).block()).isTrue();

    storage.move(source, movedTarget);
    assertThat(storage.objectExists(source).block()).isFalse();
    assertThat(storage.objectExists(movedTarget).block()).isTrue();

    storage.delete(movedTarget);
    assertThat(storage.objectExists(movedTarget).block()).isFalse();

    List<StoredObjectSummary> summaries = storage.list(bucket, "folder/");
    assertThat(summaries)
        .extracting(StoredObjectSummary::objectName)
        .containsExactlyInAnyOrder("folder/a-copy.txt", "folder/b.txt");
    assertThat(summaries).extracting(StoredObjectSummary::contentLength).containsExactly(3L, 3L);
  }
}
