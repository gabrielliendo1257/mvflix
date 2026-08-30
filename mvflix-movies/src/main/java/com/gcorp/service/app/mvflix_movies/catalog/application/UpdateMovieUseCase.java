package com.gcorp.service.app.mvflix_movies.catalog.application;

import com.gcorp.service.app.mvflix_movies.shared.application.security.UserProvider;
import com.gcorp.service.app.mvflix_movies.catalog.domain.movie.MediaKind;
import com.gcorp.service.app.mvflix_movies.catalog.domain.movie.Movie;
import com.gcorp.service.app.mvflix_movies.catalog.domain.movie.MovieAccessDeniedException;
import com.gcorp.service.app.mvflix_movies.catalog.domain.movie.MovieId;
import com.gcorp.service.app.mvflix_movies.catalog.domain.movie.MovieMetadata;
import com.gcorp.service.app.mvflix_movies.catalog.domain.movie.MovieRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import reactor.core.publisher.Mono;

import java.util.List;

/**
 * Edición manual de la metadata de una película (título, año, sinopsis, ...) sin
 * depender de la fuente externa. Solo el dueño (lo decide {@link Movie#isOwnedBy(String)});
 * el resto ve 403/404 sin revelar existencia, igual que el resto del catálogo.
 * Semántica de merge: campos {@code null} del command conservan el valor actual.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UpdateMovieUseCase {

    private final MovieRepository movieRepository;
    private final UserProvider userProvider;

    @Transactional(transactionManager = "connectionFactoryTransactionManager")
    public Mono<Movie> execute(MovieId id, UpdateMovieCommand command) {
        return this.userProvider
                .getAuthenticatedUser()
                .flatMap(user -> this.movieRepository
                        .findById(id)
                        .switchIfEmpty(Mono.error(new MovieAccessDeniedException(
                                "Movie not accessible: " + id.value())))
                        .filter(movie -> movie.isOwnedBy(user.subject()) || user.isAdmin())
                        .switchIfEmpty(Mono.error(new MovieAccessDeniedException(
                                "Movie not owned: " + id.value())))
                        .flatMap(movie -> this.update(movie, command))
                        .doOnNext(updated -> log.info(
                                "Movie {} metadata actualizada manualmente{}",
                                id.value(),
                                updated.isOwnedBy(user.subject()) ? "" : " (moderacion)")));
    }

    /**
     * Orquesta la edición: el paso a {@code VIDEO} descarta la metadata de película
     * (solo queda lo que el usuario manda, sin proveedor) y revierte a RAW, todo en
     * una sola llamada. El resto de casos hace merge normal.
     */
    private Mono<Movie> update(Movie movie, UpdateMovieCommand command) {
        boolean switchedToVideo = command.kind() == MediaKind.VIDEO
                && movie.getKind() == MediaKind.MOVIE;

        MovieMetadata merged = switchedToVideo
                ? fromCommand(command)
                : merge(movie.getMetadata(), command);

        Movie edited = switchedToVideo
                ? movie.reclassifyAsVideo(merged)
                : movie.withMetadata(merged);

        if (!switchedToVideo
                && command.kind() == MediaKind.MOVIE
                && movie.getKind() == MediaKind.VIDEO) {
            edited = edited.reclassifyAsMovie();
        }
        return this.movieRepository.updateDetails(edited);
    }

    /** Metadata solo con lo que manda el usuario (sin proveedor): para el paso a VIDEO. */
    private static MovieMetadata fromCommand(UpdateMovieCommand command) {
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
    static MovieMetadata merge(MovieMetadata current, UpdateMovieCommand command) {
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
