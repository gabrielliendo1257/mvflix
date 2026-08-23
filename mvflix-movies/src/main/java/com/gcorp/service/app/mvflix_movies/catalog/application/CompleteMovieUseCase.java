package com.gcorp.service.app.mvflix_movies.catalog.application;

import com.gcorp.service.app.mvflix_movies.shared.application.security.UserProvider;
import com.gcorp.service.app.mvflix_movies.catalog.domain.media.Media;
import com.gcorp.service.app.mvflix_movies.catalog.domain.media.MediaRepository;
import com.gcorp.service.app.mvflix_movies.catalog.domain.movie.Movie;
import com.gcorp.service.app.mvflix_movies.catalog.domain.movie.MovieConflictException;
import com.gcorp.service.app.mvflix_movies.catalog.domain.movie.MovieId;
import com.gcorp.service.app.mvflix_movies.catalog.domain.movie.MovieNotFoundException;
import com.gcorp.service.app.mvflix_movies.catalog.domain.movie.MovieRepository;
import com.gcorp.service.app.mvflix_movies.catalog.domain.movie.MovieStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;

import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
public class CompleteMovieUseCase {

  private final MovieRepository movieRepository;
  private final MediaRepository mediaRepository;
  private final UserProvider userProvider;

  @Transactional(transactionManager = "connectionFactoryTransactionManager")
  public Mono<Movie> execute(MovieId id, Long objectId, String objectKey) {
    return this.userProvider
        .getAuthenticatedUser()
        .flatMap(
            user ->
                this.movieRepository
                    .findById(id)
                    .switchIfEmpty(
                        Mono.error(new MovieNotFoundException("Movie not found: " + id.value())))
                    .filter(movie -> movie.isOwnedBy(user.subject()))
                    .switchIfEmpty(
                        Mono.error(new MovieNotFoundException("Movie not found: " + id.value())))
                    .flatMap(
                        movie ->
                            this.movieRepository
                                .completeIfDraft(id)
                                .flatMap(
                                    completed ->
                                        this.mediaRepository
                                            .save(
                                                Media.create(
                                                    completed.getId(), objectId, objectKey))
                                            .thenReturn(completed.complete(objectId)))
                                .doOnNext(
                                    completed ->
                                        log.info(
                                            "Pelicula completada: id={} owner={} object_id={} object_key={}",
                                            id.value(),
                                            user.subject(),
                                            objectId,
                                            objectKey))
                                .switchIfEmpty(this.resolveConflict(id, objectKey))));
  }

  /**
   * Reconciliación cuando el CAS no transicionó (la película ya no está en DRAFT): distingue el
   * no-op idempotente (ya READY con el mismo object_key) del 409 (estado no completable).
   */
  private Mono<Movie> resolveConflict(MovieId id, String objectKey) {
    return this.movieRepository
        .findById(id)
        .switchIfEmpty(
            Mono.error(new MovieNotFoundException("Movie not found: " + id.value())))
        .flatMap(
            movie -> {
              if (movie.getStatus() != MovieStatus.READY) {
                log.warn("Pelicula {} no completable: status={}", id.value(), movie.getStatus());
                return Mono.error(
                    new MovieConflictException("Movie is not in DRAFT state: " + id.value()));
              }
              return this.mediaRepository
                  .findByMovieId(id)
                  .flatMap(
                      media -> {
                        if (Objects.equals(objectKey, media.getObjectKey())) {
                          log.info(
                              "Pelicula {} ya READY con el mismo object_key: no-op", id.value());
                          return Mono.just(movie.complete(media.getObjectId()));
                        }
                        log.warn(
                            "Pelicula {} no completable: object_key_actual={} pedido={}",
                            id.value(),
                            media.getObjectKey(),
                            objectKey);
                        return Mono.error(
                            new MovieConflictException(
                                "Movie is not in DRAFT state: " + id.value()));
                      })
                  .switchIfEmpty(
                      Mono.defer(
                          () -> {
                            log.warn(
                                "Pelicula {} no completable: no tiene media de upload",
                                id.value());
                            return Mono.error(
                                new MovieConflictException(
                                    "Movie is not in DRAFT state: " + id.value()));
                          }));
            });
  }
}
