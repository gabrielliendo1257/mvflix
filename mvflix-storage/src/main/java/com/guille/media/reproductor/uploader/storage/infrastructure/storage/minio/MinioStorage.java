package com.guille.media.reproductor.uploader.storage.infrastructure.storage.minio;

import com.guille.media.reproductor.uploader.storage.domain.exceptions.StorageException;
import com.guille.media.reproductor.uploader.storage.domain.ports.ObjectStorageService;
import com.guille.media.reproductor.uploader.storage.domain.vos.BucketName;
import com.guille.media.reproductor.uploader.storage.domain.vos.PermissionUrl;
import com.guille.media.reproductor.uploader.storage.domain.vos.PresignedUploadRequest;
import com.guille.media.reproductor.uploader.storage.domain.vos.StorageLocation;
import com.guille.media.reproductor.uploader.storage.domain.vos.StorageMetadata;
import com.guille.media.reproductor.uploader.storage.domain.vos.StoredObjectSummary;

import io.minio.BucketExistsArgs;
import io.minio.CopyObjectArgs;
import io.minio.CopySource;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.ListObjectsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioAsyncClient;
import io.minio.MinioClient;
import io.minio.RemoveObjectArgs;
import io.minio.StatObjectArgs;
import io.minio.errors.ErrorResponseException;
import io.minio.http.Method;

import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;

import reactor.core.publisher.Mono;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

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

	private static Throwable unwrap(Throwable error) {
		return error instanceof CompletionException completionException
				? completionException.getCause()
				: error;
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

		return Mono.fromCallable(() -> this.minioAsyncClient.getPresignedObjectUrl(presigned.build()))
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
		return Mono.fromFuture(run(() -> this.minioAsyncClient.statObject(
						StatObjectArgs.builder()
								.bucket(location.bucket().bucketName())
								.object(location.storageKey().key())
								.build())))
				.map(ignored -> true)
				.onErrorResume(error -> {
					Throwable cause = unwrap(error);

					if (cause instanceof ErrorResponseException errorResponse) {
						String code = errorResponse.errorResponse().code();

						if ("NoSuchKey".equals(code)) {
							return Mono.just(false);
						}
						if ("NoSuchBucket".equals(code)) {
							return Mono.error(new StorageException(
									"Bucket '" + location.bucket() + "' does not exist.", error));
						}
					}
					return Mono.error(new StorageException(
							"Unexpected error verifying object: " + location.storageKey().key(), error));
				});
	}

	@Override
	public Mono<StorageMetadata> getMetadata(StorageLocation location) {
		return Mono.fromFuture(run(() -> this.minioAsyncClient.statObject(
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
					return new StorageException(error.getMessage(), unwrap(error));
				});
	}

	@Override
	public Mono<Boolean> bucketExists(BucketName bucketName) {
		return Mono.fromFuture(run(() -> this.minioAsyncClient.bucketExists(
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
	public void copy(StorageLocation source, StorageLocation target) {
		try {
			this.minioClient.copyObject(
					CopyObjectArgs.builder()
							.bucket(target.bucket().bucketName())
							.object(target.storageKey().key())
							.source(CopySource.builder()
									.bucket(source.bucket().bucketName())
									.object(source.storageKey().key())
									.build())
							.build());
		} catch (Exception e) {
			log.error("Error copying {} to {}", source.storageKey().key(), target.storageKey().key(), e);
			throw new StorageException("Error copying object: " + source.storageKey().key()
					+ " -> " + target.storageKey().key(), e);
		}
	}

	@Override
	public void move(StorageLocation source, StorageLocation target) {
		try {
			this.minioClient.copyObject(
					CopyObjectArgs.builder()
							.bucket(target.bucket().bucketName())
							.object(target.storageKey().key())
							.source(CopySource.builder()
									.bucket(source.bucket().bucketName())
									.object(source.storageKey().key())
									.build())
							.build());
			this.delete(source);
		} catch (StorageException e) {
			throw e;
		} catch (Exception e) {
			log.error("Error moving {} to {}", source.storageKey().key(), target.storageKey().key(), e);
			throw new StorageException("Error moving object: " + source.storageKey().key()
					+ " -> " + target.storageKey().key(), e);
		}
	}

	@Override
	public List<StoredObjectSummary> list(BucketName bucketName, String prefix) {
		try {
			Iterable<io.minio.Result<io.minio.messages.Item>> results =
					this.minioClient.listObjects(
							ListObjectsArgs.builder()
									.bucket(bucketName.bucketName())
									.prefix(prefix)
									.recursive(true)
									.build());

			List<StoredObjectSummary> objects = new java.util.ArrayList<>();
			for (io.minio.Result<io.minio.messages.Item> result : results) {
				io.minio.messages.Item item = result.get();
				objects.add(new StoredObjectSummary(
						item.objectName(),
						item.size(),
						item.etag(),
						item.lastModified().toInstant()));
			}
			return objects;
		} catch (Exception e) {
			log.error("Error listing objects with prefix {} in bucket {}", prefix,
					bucketName.bucketName(), e);
			throw new StorageException("Error listing objects with prefix: " + prefix, e);
		}
	}

	@Override
	public Mono<Void> createBucket(String nameBucket) {
		return Mono.fromFuture(run(() -> this.minioAsyncClient.makeBucket(
						MakeBucketArgs.builder()
								.bucket(nameBucket)
								.build())))
				.onErrorMap(error -> {
					log.error("Error creating bucket {}", nameBucket, error);
					return new StorageException("Error creating bucket: " + nameBucket, error);
				})
				.then();
	}
}