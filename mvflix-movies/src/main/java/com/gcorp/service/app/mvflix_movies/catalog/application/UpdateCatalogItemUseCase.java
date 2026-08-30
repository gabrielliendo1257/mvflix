package com.gcorp.service.app.mvflix_movies.catalog.application;

import com.gcorp.service.app.mvflix_movies.shared.application.security.UserProvider;
import com.gcorp.service.app.mvflix_movies.catalog.domain.movie.MediaKind;
import com.gcorp.service.app.mvflix_movies.catalog.domain.movie.CatalogItem;
import com.gcorp.service.app.mvflix_movies.catalog.domain.movie.CatalogItemAccessDeniedException;
import com.gcorp.service.app.mvflix_movies.catalog.domain.movie.CatalogItemId;
import com.gcorp.service.app.mvflix_movies.catalog.domain.movie.MovieMetadata;
import com.gcorp.service.app.mvflix_movies.catalog.domain.movie.CatalogMetadata;
import com.gcorp.service.app.mvflix_movies.catalog.domain.movie.VideoMetadata;
import com.gcorp.service.app.mvflix_movies.catalog.domain.movie.CatalogItemRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import reactor.core.publisher.Mono;

import java.util.List;

/**
 * Edición manual de la metadata de una película (título, año, sinopsis, ...) sin
 * depender de la fuente externa. Solo el dueño (lo decide {@link CatalogItem#isOwnedBy(String)});
 * el resto ve 403/404 sin revelar existencia, igual que el resto del catálogo.
 * Semántica de merge: campos {@code null} del command conservan el valor actual.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UpdateCatalogItemUseCase {

    private final CatalogItemRepository movieRepository;
    private final UserProvider userProvider;

    @Transactional(transactionManager = "connectionFactoryTransactionManager")
    public Mono<CatalogItem> execute(CatalogItemId id, UpdateCatalogItemCommand command) {
        return this.userProvider
                .getAuthenticatedUser()
                .flatMap(user -> this.movieRepository
                        .findById(id)
                        .switchIfEmpty(Mono.error(new CatalogItemAccessDeniedException(
                                "Movie not accessible: " + id.value())))
                        .filter(movie -> movie.isOwnedBy(user.subject()) || user.isAdmin())
                        .switchIfEmpty(Mono.error(new CatalogItemAccessDeniedException(
                                "CatalogItem not owned: " + id.value())))
                        .flatMap(movie -> this.update(movie, command))
                        .doOnNext(updated -> log.info(
                                "CatalogItem {} metadata actualizada manualmente{}",
                                id.value(),
                                updated.isOwnedBy(user.subject()) ? "" : " (moderacion)")));
    }

    /**
     * Orquesta la edición: el paso a {@code VIDEO} descarta la metadata de película
     * (solo queda lo que el usuario manda, sin proveedor) y revierte a RAW, todo en
     * una sola llamada. El resto de casos hace merge normal.
     */
    private Mono<CatalogItem> update(CatalogItem movie, UpdateCatalogItemCommand command) {
        boolean switchedToVideo = command.kind() == MediaKind.VIDEO
                && movie.getKind() == MediaKind.MOVIE;
        boolean switchedToMovie = command.kind() == MediaKind.MOVIE
                && movie.getKind() == MediaKind.VIDEO;

        CatalogMetadata merged = switchedToVideo
                ? fromCommand(command)
                : switchedToMovie
                        ? fromCommand(command)
                : merge(movie.getMovieMetadata(), command);

        CatalogItem edited = switchedToVideo
                ? movie.reclassifyAsVideo(merged)
                : switchedToMovie
                        ? movie.reclassifyAsMovie(merged)
                        : movie.withMetadata(merged);
        return this.movieRepository.updateDetails(edited);
    }

    /** Metadata solo con lo que manda el usuario (sin proveedor): para el paso a VIDEO. */
    private static CatalogMetadata fromCommand(UpdateCatalogItemCommand command) {
        if (command.kind() == MediaKind.VIDEO) {
            return new VideoMetadata(command.title(), command.overview(), null);
        }
        return new MovieMetadata(
                command.title(),
                command.originalTitle(),
                command.year(),
                command.genres() != null ? List.copyOf(command.genres()) : null,
                null,
                command.duration(),
                command.director(),
                command.cast() != null ? List.copyOf(command.cast()) : null,
                command.overview(),
                null,
                command.releaseDate(),
                command.country(),
                command.language(),
                command.awards() != null ? List.copyOf(command.awards()) : null,
                null);
    }

    /** Merge: cada campo del command que no venga {@code null} reemplaza al actual. */
    static MovieMetadata merge(MovieMetadata current, UpdateCatalogItemCommand command) {
        return new MovieMetadata(
                command.title() != null ? command.title() : current.title(),
                command.originalTitle() != null ? command.originalTitle() : current.originalTitle(),
                command.year() != null ? command.year() : current.year(),
                command.genres() != null ? List.copyOf(command.genres()) : current.genres(),
                command.popularity() != null ? command.popularity() : current.popularity(),
                command.duration() != null ? command.duration() : current.duration(),
                command.director() != null ? command.director() : current.director(),
                command.cast() != null ? List.copyOf(command.cast()) : current.cast(),
                command.overview() != null ? command.overview() : current.overview(),
                command.posterPath() != null ? command.posterPath() : current.posterPath(),
                command.releaseDate() != null ? command.releaseDate() : current.releaseDate(),
                command.country() != null ? command.country() : current.country(),
                command.language() != null ? command.language() : current.language(),
                command.awards() != null ? List.copyOf(command.awards()) : current.awards(),
                current.tmdbId());
    }
}
