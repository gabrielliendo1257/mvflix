package com.guille.media.reproductor.uploader.storage.infrastructure.storage.minio;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;

import org.springframework.stereotype.Service;

import com.guille.media.reproductor.uploader.storage.domain.exceptions.StorageException;
import com.guille.media.reproductor.uploader.storage.domain.ports.ObjectStorageService;
import com.guille.media.reproductor.uploader.storage.domain.vos.BucketName;
import com.guille.media.reproductor.uploader.storage.domain.vos.PermissionUrl;
import com.guille.media.reproductor.uploader.storage.domain.vos.PresignedUploadRequest;
import com.guille.media.reproductor.uploader.storage.domain.vos.StorageLocation;
import com.guille.media.reproductor.uploader.storage.domain.vos.StorageMetadata;
import com.guille.media.reproductor.uploader.storage.domain.vos.StoredObjectSummary;

import io.minio.BucketExistsArgs;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioAsyncClient;
import io.minio.StatObjectArgs;
import io.minio.StatObjectResponse;
import io.minio.errors.ErrorResponseException;
import io.minio.http.Method;
import lombok.extern.slf4j.Slf4j;

import reactor.core.publisher.Mono;

@Slf4j
@Service
public class MinioStorage implements ObjectStorageService {

	private final MinioAsyncClient minioClient;

	public MinioStorage(MinioAsyncClient minioClient) {
		this.minioClient = minioClient;
	}

	private Mono<String> getPresignedUrl(PresignedUploadRequest request, StorageLocation location, Method method) {
		var presigned = GetPresignedObjectUrlArgs.builder()
				.method(method)
				.bucket(location.bucket().bucketName())
				.object(location.storageKey().key())
				.expiry((int) request.getExpiration().toMinutes());

		if (request.getHeaders() != null && !request.getHeaders().isEmpty()) {
			presigned.extraHeaders(request.getHeaders());
		}

		return Mono.fromCallable(() -> this.minioClient.getPresignedObjectUrl(presigned.build()))
				.onErrorMap(error -> new StorageException(
						"Error generating presigned URL for " + location.storageKey().key(), error));
	}

	@Override
	public Mono<PermissionUrl> createUploadUrl(PresignedUploadRequest request, StorageLocation location) {
		return this.getPresignedUrl(request, location, Method.PUT)
				.map(url -> new PermissionUrl(url, Method.PUT.name(), request.getHeaders()));
	}

	@Override
	public Mono<PermissionUrl> createStreamingUrl(PresignedUploadRequest request, StorageLocation location) {
		return this.getPresignedUrl(request, location, Method.GET)
				.map(url -> new PermissionUrl(url, Method.GET.name(), request.getHeaders()));
	}

	@Override
	public Mono<Boolean> objectExists(StorageLocation location) {
		return Mono.fromFuture(run(() -> this.minioClient.statObject(
						StatObjectArgs.builder()
								.bucket(location.bucket().bucketName())
								.object(location.storageKey().key())
								.build())))
				.map(ignored -> true)
				.onErrorResume(ErrorResponseException.class, error -> {
					String code = error.errorResponse().code();

					if ("NoSuchKey".equals(code)) {
						return Mono.just(false);
					}
					if ("NoSuchBucket".equals(code)) {
						return Mono.error(new StorageException(
								"Bucket '" + location.bucket() + "' does not exist.", error));
					}
					return Mono.error(new StorageException("Error consulting MinIO.", error));
				})
				.onErrorMap(error -> new StorageException(
						"Unexpected error verifying object: " + location.storageKey().key(), error));
	}

	@Override
	public Mono<StorageMetadata> getMetadata(StorageLocation location) {
		return Mono.fromFuture(run(() -> this.minioClient.statObject(
						StatObjectArgs.builder()
								.bucket(location.bucket().bucketName())
								.object(location.storageKey().key())
								.build())))
				.map(metadata -> new StorageMetadata(
						metadata.contentType(),
						metadata.size(),
						metadata.etag(),
						metadata.lastModified().toInstant()))
				.onErrorMap(error -> {
					log.error("Error getting metadata for {}", location.storageKey().key(), error);
					return new StorageException(error.getMessage(), error.getCause());
				});
	}

	@Override
	public Mono<Boolean> bucketExists(BucketName bucketName) {
		return Mono.fromFuture(run(() -> this.minioClient.bucketExists(
						BucketExistsArgs.builder()
								.bucket(bucketName.bucketName())
								.build())))
				.onErrorResume(error -> {
					log.error("Error verifying bucket {}", bucketName.bucketName(), error);
					return Mono.error(new StorageException(
							"Error verifying bucket: " + bucketName.bucketName(), error));
				});
	}

	@Override
	public void delete(String key) {
	}

	@Override
	public void copy(String sourceKey, String targetKey) {
	}

	@Override
	public void move(String sourceKey, String targetKey) {
	}

	@Override
	public List<StoredObjectSummary> list(String prefix) {
		return List.of();
	}

	@Override
	public Mono<Void> createBucket(String nameBucket) {
		return Mono.fromFuture(run(() -> this.minioClient.makeBucket(
						MakeBucketArgs.builder()
								.bucket(nameBucket)
								.build())))
				.onErrorMap(error -> {
					log.error("Error creating bucket {}", nameBucket, error);
					return new StorageException("Error creating bucket: " + nameBucket, error);
				})
				.then();
	}

	/**
	 * Ejecuta una llamada async de MinIO capturando las checked exceptions que
	 * declara su firma y devolviéndolas como un future fallido, para poder
	 * componerlas con {@link Mono#fromFuture(CompletableFuture)}.
	 */
	private static <T> CompletableFuture<T> run(Callable<CompletableFuture<T>> action) {
		try {
			return action.call();
		} catch (Exception e) {
			return CompletableFuture.failedFuture(e);
		}
	}
}