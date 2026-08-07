package com.guille.media.reproductor.uploader.storage.infrastructure.storage.minio;

import java.util.List;

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
import io.minio.MinioClient;
import io.minio.StatObjectArgs;
import io.minio.StatObjectResponse;
import io.minio.errors.ErrorResponseException;
import io.minio.http.Method;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Slf4j
@Service
public class MinioStorage implements ObjectStorageService {

	private final MinioClient minioClient;

	public MinioStorage(MinioClient minioClient) {
		this.minioClient = minioClient;
	}

	private String getPresignedUrl(PresignedUploadRequest request, StorageLocation location, Method method)
			throws Exception {
		var presigned = GetPresignedObjectUrlArgs.builder()
				.method(method)
				.bucket(location.bucket().bucketName())
				.object(location.storageKey().key())
				.expiry((int) request.getExpiration().toMinutes());

		if (request.getHeaders() != null && !request.getHeaders().isEmpty()) {
			presigned.extraHeaders(request.getHeaders());
		}

		return this.minioClient.getPresignedObjectUrl(presigned.build());
	}

	@Override
	public Mono<PermissionUrl> createUploadUrl(PresignedUploadRequest request, StorageLocation location) {
		return Mono.fromCallable(() -> {
			Method method = Method.PUT;
			String presignedUrl = this.getPresignedUrl(request, location, method);
			return new PermissionUrl(presignedUrl, method.name(), request.getHeaders());
		})
		.subscribeOn(Schedulers.boundedElastic())
		.onErrorMap(e -> new StorageException("Error creando URL presignada de carga: " + e.getMessage(), e));
	}

	@Override
	public Mono<PermissionUrl> createStreamingUrl(PresignedUploadRequest request, StorageLocation location) {
		return Mono.fromCallable(() -> {
			Method method = Method.GET;
			String presignedUrl = this.getPresignedUrl(request, location, method);
			return new PermissionUrl(presignedUrl, method.name(), request.getHeaders());
		})
		.subscribeOn(Schedulers.boundedElastic())
		.onErrorMap(e -> new StorageException("Error creando URL presignada de streaming: " + e.getMessage(), e));
	}

	@Override
	public Mono<Boolean> objectExists(StorageLocation location) {
		return Mono.fromCallable(() -> {
			try {
				StatObjectResponse response = this.minioClient.statObject(
						StatObjectArgs.builder()
								.bucket(location.bucket().bucketName())
								.object(location.storageKey().key())
								.build());
				return response != null;
			} catch (ErrorResponseException e) {
				if ("NoSuchKey".equals(e.errorResponse().code())) {
					return false;
				}
				if ("NoSuchBucket".equals(e.errorResponse().code())) {
					throw new StorageException("El bucket '" + location.bucket() + "' no existe.", e);
				}
				throw new StorageException("Error consultando MinIO: " + e.getMessage(), e);
			} catch (Exception e) {
				throw new StorageException("Error inesperado verificando objeto.", e);
			}
		})
		.subscribeOn(Schedulers.boundedElastic());
	}

	@Override
	public Mono<StorageMetadata> getMetadata(StorageLocation location) {
		return Mono.fromCallable(() -> {
			try {
				StatObjectResponse metadata = this.minioClient.statObject(
						StatObjectArgs.builder()
								.bucket(location.bucket().bucketName())
								.object(location.storageKey().key())
								.build());
				return new StorageMetadata(
						metadata.contentType(),
						metadata.size(),
						metadata.etag(),
						metadata.lastModified().toInstant());
			} catch (Exception e) {
				log.error("Error al obtener la metadata", e);
				throw new StorageException(e.getMessage(), e.getCause());
			}
		})
		.subscribeOn(Schedulers.boundedElastic());
	}

	@Override
	public Mono<Boolean> bucketExists(BucketName bucketName) {
		return Mono.fromCallable(() -> {
			try {
				return this.minioClient.bucketExists(
						BucketExistsArgs.builder()
								.bucket(bucketName.bucketName())
								.build());
			} catch (Exception e) {
				log.error("Error al verificar el bucket {}", bucketName.bucketName(), e);
				return false;
			}
		})
		.subscribeOn(Schedulers.boundedElastic());
	}

	@Override
	public Mono<Void> delete(String key) {
		return Mono.error(new UnsupportedOperationException("Operación 'delete' no implementada aún."));
	}

	@Override
	public Mono<Void> copy(String sourceKey, String targetKey) {
		return Mono.error(new UnsupportedOperationException("Operación 'copy' no implementada aún."));
	}

	@Override
	public Mono<Void> move(String sourceKey, String targetKey) {
		return Mono.error(new UnsupportedOperationException("Operación 'move' no implementada aún."));
	}

	@Override
	public Mono<List<StoredObjectSummary>> list(String prefix) {
		return Mono.just(List.of());
	}

	@Override
	public Mono<Void> createBucket(String nameBucket) {
		return Mono.<Void>fromRunnable(() -> {
			try {
				this.minioClient.makeBucket(
						MakeBucketArgs.builder()
								.bucket(nameBucket)
								.build());
			} catch (Exception ex) {
				log.error("Error al crear el bucket {}", nameBucket, ex);
				throw new StorageException(ex.getMessage(), ex.getCause());
			}
		})
		.subscribeOn(Schedulers.boundedElastic());
	}
}
