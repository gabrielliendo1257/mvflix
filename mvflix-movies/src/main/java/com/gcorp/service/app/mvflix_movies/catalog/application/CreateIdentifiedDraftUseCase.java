package com.gcorp.service.app.mvflix_movies.catalog.application;

import com.gcorp.service.app.mvflix_movies.catalog.domain.movie.MediaKind;
import com.gcorp.service.app.mvflix_movies.catalog.domain.movie.Movie;
import com.gcorp.service.app.mvflix_movies.catalog.domain.movie.MovieMetadata;
import com.gcorp.service.app.mvflix_movies.catalog.domain.movie.MovieRepository;
import com.gcorp.service.app.mvflix_movies.catalog.domain.movie.MovieVisibility;
import com.gcorp.service.app.mvflix_movies.shared.application.security.UserProvider;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;

import reactor.core.publisher.Mono;

import java.util.List;

/**
 * Alta guiada (Add Media): crea el DRAFT ya identificado —metadata del
 * preview + tmdbId— y aplica el acceso inicial (visibilidad + compartidos)
 * como UNA SOLA unidad transaccional. El BFF coordina la experiencia;
 * las reglas de visibilidad y de identidad del proveedor las valida Movies.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CreateIdentifiedDraftUseCase {

    private final MovieRepository movieRepository;
    private final UserProvider userProvider;

    public Mono<Movie> execute(CreateIdentifiedDraftCommand command) {
        MovieVisibility visibility = command.visibility() == null
                ? MovieVisibility.PRIVATE
                : command.visibility();
        List<String> cleanShared = clean(command.sharedWith());
        if (visibility == MovieVisibility.SHARED && cleanShared.isEmpty()) {
            return Mono.error(new IllegalArgumentException(
                    "SHARED requiere al menos un username en usernames"));
        }
        return this.userProvider
                .getAuthenticatedUser()
                .flatMap(user -> {
                    Movie draft = buildIdentifiedDraft(user.subject(), command)
                            .withVisibility(visibility)
                            .withSharedWith(java.util.Set.copyOf(cleanShared));
                    return this.movieRepository.saveDraftWithAccess(draft);
                })
                .doOnNext(saved -> log.info(
                        "Identified draft creado: id={} tmdb={} visibility={} shared={}",
                        saved.getId().value(), saved.getMetadata().tmdbId(),
                        saved.getVisibility(), saved.getSharedWith().size()));
    }

    /**
     * Identidad del proveedor: para MOVIE el tmdbId es obligatorio (el flujo
     * guiado nace de un candidato); VIDEO no se identifica con TMDB.
     */
    private Movie buildIdentifiedDraft(String owner, CreateIdentifiedDraftCommand command) {
        MediaKind kind = command.kind() == null ? MediaKind.MOVIE : command.kind();
        Movie draft = Movie.createDraft(owner, command.metadata(), kind);
        if (kind == MediaKind.MOVIE) {
            if (command.metadata().tmdbId() == null) {
                throw new IllegalArgumentException(
                        "tmdbId es obligatorio para crear un draft identificado de película");
            }
            return draft.linkProviderMetadata(command.metadata());
        }
        return draft;
    }

    private static List<String> clean(List<String> usernames) {
        return usernames == null
                ? List.of()
                : usernames.stream()
                        .filter(u -> u != null && !u.isBlank())
                        .distinct()
                        .toList();
    }
}
