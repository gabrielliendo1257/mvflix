package com.guille.media.reproductor.uploader.storage.app.service;

import com.guille.media.reproductor.uploader.storage.app.commands.requets.CreateUploadCommand;
import com.guille.media.reproductor.uploader.storage.app.commands.requets.StreamingCommand;
import com.guille.media.reproductor.uploader.storage.app.commands.response.StreamingSession;
import com.guille.media.reproductor.uploader.storage.app.commands.response.UploadSession;
import com.guille.media.reproductor.uploader.storage.app.security.UserProvider;
import com.guille.media.reproductor.uploader.storage.app.user.UserServiceCommandPort;
import com.guille.media.reproductor.uploader.storage.domain.exceptions.BucketNotFoundException;
import com.guille.media.reproductor.uploader.storage.domain.exceptions.InvalidObjectCopntentError;
import com.guille.media.reproductor.uploader.storage.domain.exceptions.StorageObjectNotAvailable;
import com.guille.media.reproductor.uploader.storage.domain.models.StorageKeyGenerator;
import com.guille.media.reproductor.uploader.storage.domain.models.StoreObject;
import com.guille.media.reproductor.uploader.storage.domain.models.StoreObject.StorageSessionStatus;
import com.guille.media.reproductor.uploader.storage.domain.models.UploadConfiguration;
import com.guille.media.reproductor.uploader.storage.domain.ports.ObjectStorageService;
import com.guille.media.reproductor.uploader.storage.domain.ports.StorageRepository;
import com.guille.media.reproductor.uploader.storage.domain.ports.UserStorageRepository;
import com.guille.media.reproductor.uploader.storage.domain.service.StorageService;
import com.guille.media.reproductor.uploader.storage.domain.service.UploadPolicy;
import com.guille.media.reproductor.uploader.storage.domain.vos.BucketName;
import com.guille.media.reproductor.uploader.storage.domain.vos.MimeType;
import com.guille.media.reproductor.uploader.storage.domain.vos.PermissionUrl;
import com.guille.media.reproductor.uploader.storage.domain.vos.PresignedUploadRequest;
import com.guille.media.reproductor.uploader.storage.domain.vos.StorageLocation;
import com.guille.media.reproductor.uploader.storage.domain.vos.StorageMetadata;

import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;

import reactor.core.publisher.Mono;

import java.time.Instant;

@Slf4j
@Service
public class AppStorageService implements StorageService {

    private final ObjectStorageService objectStoragePort;
    private final StorageKeyGenerator storageKeyGenerator;
    private final UploadPolicy uploadPolicy;
    private final StorageRepository storageRepository;
    private final UserServiceCommandPort userServiceQueryPort;
    private final UserProvider userProvider;
    private final UserStorageRepository userStorageRepository;

    public AppStorageService(
            ObjectStorageService objectStorageService,
            StorageKeyGenerator storageKeyGenerator,
            UploadPolicy uploadPolicy,
            StorageRepository storageRepository,
            UserServiceCommandPort userServiceQueryPort,
            UserProvider userProvider,
            UserStorageRepository userStorageRepository) {
        this.objectStoragePort = objectStorageService;
        this.storageKeyGenerator = storageKeyGenerator;
        this.uploadPolicy = uploadPolicy;
        this.storageRepository = storageRepository;
        this.userServiceQueryPort = userServiceQueryPort;
        this.userProvider = userProvider;
        this.userStorageRepository = userStorageRepository;
    }

    @Override
    public Mono<UploadSession> createUploadSession(CreateUploadCommand createUploadCommand) {
        log.info("Starting upload session: {}", createUploadCommand);

        return this.userProvider
                .getAuthenticatedUser()
                .flatMap(
                        authenticatedUser ->
                                this.userStorageRepository.findByOwnerUsername(
                                        authenticatedUser.subject()))
                .map(
                        userStorage -> {
                            BucketName userBucket = userStorage.getBucketName();
                            if (!this.objectStoragePort.bucketExists(userBucket)) {
                                Mono.error(
                                        new BucketNotFoundException(
                                                "Bucket not found: " + userBucket));
                            }
                            UploadConfiguration uploadConfiguration =
                                    this.uploadPolicy.resolve(
                                            createUploadCommand.size(),
                                            createUploadCommand.mimeType());
                            PresignedUploadRequest presignedUploadRequest =
                                    new PresignedUploadRequest(uploadConfiguration.expiration());
                            presignedUploadRequest.setContentType(createUploadCommand.mimeType());

                            PermissionUrl permissionUrl =
                                    this.objectStoragePort.createUploadUrl(
                                            presignedUploadRequest,
                                            new StorageLocation(
                                                    userBucket,
                                                    this.storageKeyGenerator.generate()));
                            userStorage.consumeStorage(createUploadCommand.size());
                        });

        //        BucketName bucket = BucketName.of("users"); // TODO No hard codear el nombre
        //        if (!this.objectStoragePort.bucketExists(bucket)) {
        //            throw new BucketNotFoundException("Bucket not found: " + bucket.bucketName());
        //        }
        //
        //        StorageKey key = storageKeyGenerator.generate();
        //        StorageLocation location = new StorageLocation(bucket, key);
        //        if (this.objectStoragePort.objectExists(location)) {
        //            throw new ObjectAlreadyExistsException(key);
        //        }
        //        UploadConfiguration uploadConfiguration =
        //                this.uploadPolicy.resolve(
        //                        createUploadCommand.size(), createUploadCommand.mimeType());
        //        PresignedUploadRequest uploadRequest =
        //                new PresignedUploadRequest(
        //                        uploadConfiguration.expiration(),
        //                        Map.of("Content-Type", uploadConfiguration.mimeType().value()));
        //
        //        PermissionUrl uploadUrl = this.objectStoragePort.createUploadUrl(uploadRequest,
        // location);
        //
        //        StorageMetadata storageMetadata =
        //                new StorageMetadata(
        //                        createUploadCommand.mimeType().value(),
        //                        createUploadCommand.size(),
        //                        "test_checksum",
        //                        Instant.now());
        //        StoreObject storageObject =
        //                this.storageRepository.save(
        //                        new StoreObject(
        //                                location,
        //                                storageMetadata,
        //                                UploadId.generate(),
        //                                authenticatedUser,
        //                                StorageSessionStatus.PENDING));
        //
        //        UploadSession uploadSession =
        //                new UploadSession(
        //                        storageObject.getStorageId(),
        //                        uploadUrl.presignedUrl(),
        //                        key,
        //                        uploadUrl.method(),
        //                        new ExpectedObjectData(
        //                                storageObject.sizeInBytes(),
        //                                createUploadCommand.mimeType().value(),
        //                                storageObject.status,
        //                                Instant.now().plus(uploadConfiguration.expiration())));
        //        log.info("Success upload session, returning: {}", uploadSession);
        //        return uploadSession;
    }

    @Override
    public StreamingSession generateStreamingSession(StreamingCommand command) {
        StoreObject storageObjectFromRepository =
                this.storageRepository.findById(command.objectId());
        if (!storageObjectFromRepository.isAvailable()) {
            throw new StorageObjectNotAvailable(
                    "Storage object not available: " + command.objectId());
        }
        UploadConfiguration uploadConfiguration =
                this.uploadPolicy.resolve(
                        storageObjectFromRepository.sizeInBytes(),
                        MimeType.of(storageObjectFromRepository.getMetadata().contentType()));
        PresignedUploadRequest request =
                new PresignedUploadRequest(uploadConfiguration.expiration(), null);
        PermissionUrl permissionUrl =
                this.objectStoragePort.createStreamingUrl(
                        request, storageObjectFromRepository.getStorageKey());

        return new StreamingSession(
                storageObjectFromRepository.getStorageId(),
                permissionUrl.presignedUrl(),
                storageObjectFromRepository.getStorageKey().storageKey(),
                Instant.now().plus(uploadConfiguration.expiration()),
                permissionUrl.method());
    }

    @Override
    public void completeUpload(String uploadId) {
        StoreObject storageObject = this.storageRepository.findById(uploadId);

        if (!this.objectStoragePort.objectExists(storageObject.getStorageKey())) {
            throw new StorageObjectNotAvailable("Storage object not available: " + uploadId);
        }

        StorageMetadata metadata =
                this.objectStoragePort.getMetadata(storageObject.getStorageKey());
        log.info("Metadata from s3: {}", metadata);
        if (metadata.contentLength() != storageObject.getMetadata().contentLength()) {
            throw new InvalidObjectCopntentError();
        }

        storageObject.status = StorageSessionStatus.COMPLETED;
        log.info("Completed upload {}", storageObject);
        this.storageRepository.save(storageObject);
    }
}
