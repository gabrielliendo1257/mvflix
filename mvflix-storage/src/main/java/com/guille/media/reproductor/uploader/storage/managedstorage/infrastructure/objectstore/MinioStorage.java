package com.guille.media.reproductor.uploader.storage.managedstorage.infrastructure.objectstore;

import com.guille.media.reproductor.uploader.storage.managedstorage.domain.exception.StorageException;
import com.guille.media.reproductor.uploader.storage.managedstorage.domain.port.ObjectStorageService;
import com.guille.media.reproductor.uploader.storage.managedstorage.domain.model.BucketName;
import com.guille.media.reproductor.uploader.storage.managedstorage.domain.model.PermissionUrl;
import com.guille.media.reproductor.uploader.storage.managedstorage.domain.model.PresignedUploadRequest;
import com.guille.media.reproductor.uploader.storage.managedstorage.domain.model.StorageLocation;
import com.guille.media.reproductor.uploader.storage.managedstorage.domain.model.StorageMetadata;
import io.minio.BucketExistsArgs;
import io.minio.CopyObjectArgs;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioAsyncClient;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import io.minio.StatObjectArgs;
import io.minio.errors.ErrorResponseException;
import io.minio.http.Method;
import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import com.guille.media.reproductor.uploader.storage.managedstorage.domain.model.StorageFolder;

@Slf4j
@Service
public class MinioStorage implements ObjectStorageService {

  private final MinioAsyncClient minioAsyncClient;
  private final MinioClient minioClient;

  public MinioStorage(MinioAsyncClient minioAsyncClient, MinioClient minioClient) {
    this.minioAsyncClient = minioAsyncClient;
    this.minioClient = minioClient;
  }

  /**
   * Ejecuta una llamada async de MinIO capturando las checked exceptions que declara su firma y
   * devolviéndolas como un future fallido, para poder componerlas con {@link
   * Mono#fromFuture(CompletableFuture)}.
   */
  private static <T> CompletableFuture<T> run(Callable<CompletableFuture<T>> action) {
    try {
      return action.call();
    } catch (Exception e) {
      return CompletableFuture.failedFuture(e);
    }
  }

  private static Throwable unwrap(Throwable error) {
    return error instanceof CompletionException completionException
        ? completionException.getCause()
        : error;
  }

  private Mono<String> getPresignedUrl(
      PresignedUploadRequest request, StorageLocation location, Method method) {
    var presigned =
        GetPresignedObjectUrlArgs.builder()
            .method(method)
            .bucket(location.bucket().bucketName())
            .object(location.storageKey().key())
            .expiry((int) request.getExpiration().toMinutes(), TimeUnit.MINUTES);

    if (request.getHeaders() != null && !request.getHeaders().isEmpty()) {
      presigned.extraHeaders(request.getHeaders());
    }

    return Mono.fromCallable(() -> this.minioAsyncClient.getPresignedObjectUrl(presigned.build()))
        .onErrorMap(
            error ->
                new StorageException(
                    "Error generating presigned URL for " + location.storageKey().key(), error));
  }

  @Override
  public Mono<PermissionUrl> createUploadUrl(
      PresignedUploadRequest request, StorageLocation location) {
    return this.getPresignedUrl(request, location, Method.PUT)
        .map(url -> new PermissionUrl(url, Method.PUT.name(), request.getHeaders()));
  }

  @Override
  public Mono<PermissionUrl> createStreamingUrl(
      PresignedUploadRequest request, StorageLocation location) {
    return this.getPresignedUrl(request, location, Method.GET)
        .map(url -> new PermissionUrl(url, Method.GET.name(), request.getHeaders()));
  }

  @Override
  public Mono<Boolean> objectExists(StorageLocation location) {
    return Mono.fromFuture(
            run(
                () ->
                    this.minioAsyncClient.statObject(
                        StatObjectArgs.builder()
                            .bucket(location.bucket().bucketName())
                            .object(location.storageKey().key())
                            .build())))
        .map(ignored -> true)
        .onErrorResume(
            error -> {
              Throwable cause = unwrap(error);

              if (cause instanceof ErrorResponseException errorResponse) {
                String code = errorResponse.errorResponse().code();

                if ("NoSuchKey".equals(code)) {
                  return Mono.just(false);
                }
                if ("NoSuchBucket".equals(code)) {
                  return Mono.error(
                      new StorageException(
                          "Bucket '" + location.bucket() + "' does not exist.", error));
                }
              }
              return Mono.error(
                  new StorageException(
                      "Unexpected error verifying object: " + location.storageKey().key(), error));
            });
  }

  @Override
  public Mono<StorageMetadata> getMetadata(StorageLocation location) {
    return Mono.fromFuture(
            run(
                () ->
                    this.minioAsyncClient.statObject(
                        StatObjectArgs.builder()
                            .bucket(location.bucket().bucketName())
                            .object(location.storageKey().key())
                            .build())))
        .map(
            metadata ->
                new StorageMetadata(
                    metadata.contentType(),
                    metadata.size(),
                    metadata.etag(),
                    metadata.lastModified().toInstant()))
        .onErrorMap(
            error -> {
              log.error("Error getting metadata for {}", location.storageKey().key(), error);
              return new StorageException(error.getMessage(), unwrap(error));
            });
  }

  @Override
  public Mono<Boolean> bucketExists(BucketName bucketName) {
    return Mono.fromFuture(
            run(
                () ->
                    this.minioAsyncClient.bucketExists(
                        BucketExistsArgs.builder().bucket(bucketName.bucketName()).build())))
        .onErrorResume(
            error -> {
              log.error("Error verifying bucket {}", bucketName.bucketName(), error);
              return Mono.error(
                  new StorageException(
                      "Error verifying bucket: " + bucketName.bucketName(), error));
            });
  }

  @Override
  public void delete(StorageLocation location) {
    try {
      this.minioClient.removeObject(
          RemoveObjectArgs.builder()
              .bucket(location.bucket().bucketName())
              .object(location.storageKey().key())
              .build());
    } catch (Exception e) {
      log.error("Error deleting object {}", location.storageKey().key(), e);
      throw new StorageException("Error deleting object: " + location.storageKey().key(), e);
    }
  }

  @Override
  public Mono<Void> createBucket(String nameBucket) {
    return Mono.fromFuture(
            run(
                () ->
                    this.minioAsyncClient.makeBucket(
                        MakeBucketArgs.builder().bucket(nameBucket).build())))
        .onErrorMap(
            error -> {
              log.error("Error creating bucket {}", nameBucket, error);
              return new StorageException("Error creating bucket: " + nameBucket, error);
            })
        .then();
  }

  @Override
  public Mono<Void> ensureBucket(BucketName bucketName) {
    return this.bucketExists(bucketName)
        .flatMap(exists -> exists ? Mono.empty() : this.createBucket(bucketName.bucketName()));
  }

  @Override
  public Mono<Void> ensureUserStorageLayout(BucketName bucketName, String username) {
    return Mono.when(
        java.util.Arrays.stream(StorageFolder.values())
            .map(folder -> this.putEmptyFolder(bucketName, username + "/" + folder.path()))
            .toList());
  }

  private Mono<Void> putEmptyFolder(BucketName bucketName, String objectKey) {
    return Mono.fromFuture(
            run(
                () ->
                    this.minioAsyncClient.putObject(
                        PutObjectArgs.builder()
                            .bucket(bucketName.bucketName())
                            .object(objectKey)
                            .stream(new ByteArrayInputStream(new byte[0]), 0, -1)
                            .build())))
        .onErrorMap(
            error -> {
              log.error("Error ensuring storage folder {}/{}", bucketName, objectKey, error);
              return new StorageException(
                  "Error ensuring storage folder: " + objectKey, error);
            })
        .then();
  }
}
