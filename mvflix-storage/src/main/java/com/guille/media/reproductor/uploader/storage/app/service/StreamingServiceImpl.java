package com.guille.media.reproductor.uploader.storage.app.service;

import java.time.Duration;
import java.time.Instant;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.stereotype.Service;

import com.guille.media.reproductor.uploader.storage.app.commands.requests.StreamingCommand;
import com.guille.media.reproductor.uploader.storage.shared.security.UserProvider;
import com.guille.media.reproductor.uploader.storage.app.commands.response.StreamingSession;
import com.guille.media.reproductor.uploader.storage.domain.exceptions.StorageObjectNotAvailable;
import com.guille.media.reproductor.uploader.storage.domain.exceptions.UserStorageNotFoundException;
import com.guille.media.reproductor.uploader.storage.domain.models.StoreObject;
import com.guille.media.reproductor.uploader.storage.domain.ports.ObjectStorageService;
import com.guille.media.reproductor.uploader.storage.domain.ports.StorageRepository;
import com.guille.media.reproductor.uploader.storage.domain.ports.UserStorageRepository;
import com.guille.media.reproductor.uploader.storage.domain.vos.PresignedUploadRequest;
import com.guille.media.reproductor.uploader.storage.domain.vos.StorageLocation;

import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@Slf4j
@Service
public class StreamingServiceImpl implements StreamingService {

  /**
   * TTL de la URL presigned de streaming: 1 hora cubre una película completa (si se agota, el front
   * renueva con una nueva sesión de streaming). No reutiliza el TTL de la politica de upload (30
   * min / 6 h): son vidas distintas (subir archivo vs. reproducir).
   */
  /**
   * TTL de la URL presigned de streaming, configurable vía
   * {@code storage.streaming.url-ttl} (ISO-8601, por defecto 3h). Debe cubrir la
   * reproducción más larga esperada; si se agota, el front solicita una nueva
   * sesión. No reutiliza el TTL de la política de upload: son vidas distintas
   * (subir archivo vs. reproducir).
   */
  private final Duration streamingUrlTtl;

  private final UserProvider userProvider;
  private final ObjectStorageService objectStoragePort;
  private final StorageRepository storageRepository;
  private final UserStorageRepository userStorageRepository;

  public StreamingServiceImpl(
      UserProvider userProvider,
      ObjectStorageService objectStorageService,
      StorageRepository storageRepository,
      UserStorageRepository userStorageRepository,
      @Value("${storage.streaming.url-ttl:PT3H}") Duration streamingUrlTtl) {
    this.userProvider = userProvider;
    this.objectStoragePort = objectStorageService;
    this.storageRepository = storageRepository;
    this.userStorageRepository = userStorageRepository;
    this.streamingUrlTtl = streamingUrlTtl;
  }

  @Override
  public Mono<StreamingSession> generateStreamingSession(StreamingCommand command) {
    return Mono.fromCallable(() -> Long.parseLong(command.objectId()))
        .onErrorMap(
            NumberFormatException.class,
            ex -> new IllegalArgumentException("Invalid objectId: " + command.objectId()))
        .flatMap(this.storageRepository::findById)
        .switchIfEmpty(
            Mono.error(
                new StorageObjectNotAvailable(
                    "Storage object not available: " + command.objectId())))
        .flatMap(
            object ->
                this.userProvider
                    .getAuthenticatedUser()
                    .switchIfEmpty(
                        Mono.error(new AuthenticationCredentialsNotFoundException("No authenticated user")))
                    .map(user -> {
                      object.ensureOwnedBy(user.subject());
                      return object;
                    }))
        .flatMap(this::createStreamingSession)
        .doOnNext(
            session -> log.info("Streaming session created: uploadId={}", session.uploadId()));
  }

  @Override
  public Mono<StreamingSession> generateCatalogStreamingSession(StreamingCommand command) {
    return Mono.fromCallable(() -> Long.parseLong(command.objectId()))
        .onErrorMap(
            NumberFormatException.class,
            ex -> new IllegalArgumentException("Invalid objectId: " + command.objectId()))
        .flatMap(this.storageRepository::findById)
        .switchIfEmpty(
            Mono.error(
                new StorageObjectNotAvailable(
                    "Storage object not available: " + command.objectId())))
        .flatMap(this::createStreamingSession)
        .doOnNext(
            session ->
                log.info("Catalog streaming session created: uploadId={}", session.uploadId()));
  }

  private Mono<StreamingSession> createStreamingSession(StoreObject object) {
    object.ensureAvailable();

    return this.userStorageRepository
        .findByOwnerUsername(object.getOwnerUsername())
        .switchIfEmpty(
            Mono.error(
                new UserStorageNotFoundException(
                    "No storage registered for user: " + object.getOwnerUsername())))
        .flatMap(
            userStorage -> {
              StorageLocation location =
                  new StorageLocation(userStorage.getBucketName(), object.getStorageKey());
              PresignedUploadRequest request = new PresignedUploadRequest(this.streamingUrlTtl);

              return this.objectStoragePort
                  .createStreamingUrl(request, location)
                  .flatMap(
                      permissionUrl ->
                          this.storageRepository
                              .touchLastSeen(object.getStorageId(), Instant.now())
                              .onErrorResume(
                                  error -> {
                                    log.warn(
                                        "Failed to record last-seen for storageId={}",
                                        object.getStorageId(),
                                        error);
                                    return Mono.empty();
                                  })
                              .thenReturn(
                                  new StreamingSession(
                                      String.valueOf(object.getStorageId()),
                                      permissionUrl.presignedUrl(),
                                      object.getStorageKey(),
                                      Instant.now().plus(this.streamingUrlTtl),
                                      permissionUrl.method())));
            });
  }
}
