package com.gcorp.service.app.mvflix_movies.application.movie;

import com.gcorp.service.app.mvflix_movies.app.security.UserProvider;
import com.gcorp.service.app.mvflix_movies.library.domain.MediaAsset;
import com.gcorp.service.app.mvflix_movies.library.domain.MediaAssetRepository;
import com.gcorp.service.app.mvflix_movies.catalog.domain.movie.Movie;
import com.gcorp.service.app.mvflix_movies.catalog.domain.movie.MovieId;
import com.gcorp.service.app.mvflix_movies.catalog.domain.movie.MovieRepository;
import com.gcorp.service.app.mvflix_movies.catalog.domain.movie.MovieVisibility;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * Cambia la visibilidad de varias peliculas del catalogo en un solo lote.
 * Los ids pueden venir directos (movieIds) o derivarse de librerias
 * (libraryIds resuelve los assets identificados de cada libreria).
 * Solo se procesan las peliculas del dueño autenticado: las ajenas ni se
 * consultan. SHARED exige al menos un username; los que fallan por cualquier
 * motivo se cuentan como failed y no abortan el lote.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BulkVisibilityUseCase {

    private final MovieRepository movieRepository;
    private final MediaAssetRepository assetRepository;
    private final UserProvider userProvider;

    public Mono<BulkVisibilityResult> execute(
            List<MovieId> movieIds, List<Long> libraryIds,
            MovieVisibility visibility, List<String> usernames) {
        if (visibility == MovieVisibility.SHARED
                && (usernames == null
                        || usernames.stream().noneMatch(u -> u != null && !u.isBlank()))) {
            return Mono.error(new IllegalArgumentException(
                    "SHARED requiere al menos un username en usernames"));
        }
        List<String> clean = usernames == null
                ? List.of()
                : usernames.stream()
                        .filter(u -> u != null && !u.isBlank())
                        .distinct()
                        .toList();
        return this.userProvider
                .getAuthenticatedUser()
                .flatMapMany(user -> Flux.concat(
                                Flux.fromIterable(movieIds),
                                Flux.fromIterable(libraryIds)
                                        .flatMap(libraryId -> this.assetRepository
                                                .findAllByLibraryId(libraryId))
                                        .filter(asset -> asset.getMovieId() != null)
                                        .map(MediaAsset::getMovieId))
                        .distinct()
                        .collectList()
                        .flatMapMany(ids -> ids.isEmpty()
                                ? Flux.empty()
                                : this.movieRepository
                                        .findByOwnerAndIds(user.subject(), ids)))
                .flatMap(movie -> this.applyVisibility(movie, visibility, clean)
                        .map(updated -> true)
                        .onErrorResume(error -> {
                            log.warn("Bulk: movie {} no se pudo actualizar: {}",
                                    movie.getId().value(), error.getMessage());
                            return Mono.just(false);
                        }), 16)
                .collectList()
                .map(results -> {
                    int updated = (int) results.stream().filter(Boolean.TRUE::equals).count();
                    return new BulkVisibilityResult(
                            results.size(), updated, results.size() - updated);
                })
                .doOnNext(result -> log.info(
                        "Bulk visibilidad {} -> {}, procesadas: {}, actualizadas: {}, fallidas: {}",
                        visibility, clean, result.total(), result.updated(), result.failed()));
    }

    private Mono<Movie> applyVisibility(
            Movie movie, MovieVisibility visibility, List<String> usernames) {
        Mono<Movie> update = this.movieRepository.updateVisibility(movie.getId(), visibility);
        if (visibility == MovieVisibility.SHARED) {
            return update.flatMap(updated ->
                    this.movieRepository.replaceShares(updated.getId(), usernames));
        }
        return update;
    }
}