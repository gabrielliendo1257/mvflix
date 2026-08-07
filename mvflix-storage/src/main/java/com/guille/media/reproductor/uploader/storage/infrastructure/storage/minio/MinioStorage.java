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

		return this.minioClient.getPresignedObjectUrl(
				presigned.build());
	}

	@Override
	public PermissionUrl createUploadUrl(PresignedUploadRequest request, StorageLocation location) {
		Method method = Method.PUT;
		try {
			String presignedUrl = this.getPresignedUrl(request, location, method);

			return new PermissionUrl(presignedUrl, method.name(), request.getHeaders());
		} catch (Exception e) {
			throw new StorageException(e.getMessage(), e.getCause());
		}
	}

	@Override
	public PermissionUrl createStreamingUrl(PresignedUploadRequest request, StorageLocation location) {
		Method method = Method.GET;
		try {
			String presignedUrl = this.getPresignedUrl(request, location, method);

			return new PermissionUrl(presignedUrl, method.name(), request.getHeaders());
		} catch (Exception e) {
			throw new StorageException(e.getMessage(), e.getCause());
		}
	}

	@Override
	public boolean objectExists(StorageLocation location) {
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
				throw new RuntimeException("El bucket '" + location.bucket() + "' no existe.", e);
			}

			throw new RuntimeException("Error consultando MinIO.", e);
		} catch (Exception e) {
			throw new RuntimeException("Error inesperado verificando objeto.", e);
		}

	}

	@Override
	public StorageMetadata getMetadata(StorageLocation location) {
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
	}

	@Override
	public boolean bucketExists(BucketName bucketName) {
		try {
			return this.minioClient.bucketExists(
					BucketExistsArgs.builder()
							.bucket(bucketName.bucketName())
							.build());
		} catch (Exception e) {
			log.error("Error al verificar el bucket {}", bucketName.bucketName(), e);
			return false;
		}
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
	public void createBucket(String nameBucket) {
		try {
			this.minioClient.makeBucket(
					MakeBucketArgs.builder()
							.bucket(nameBucket)
							.build());
		} catch (Exception ex) {
			log.error("Error al crear el bucket {}", nameBucket, ex);
			throw new StorageException(ex.getMessage(), ex.getCause());
		}
	}
}
