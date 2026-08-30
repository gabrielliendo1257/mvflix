package com.gcorp.service.app.mvflix_movies.catalog.application;

import com.gcorp.service.app.mvflix_movies.catalog.domain.movie.MediaKind;
import com.gcorp.service.app.mvflix_movies.catalog.domain.movie.CatalogItem;
import com.gcorp.service.app.mvflix_movies.catalog.domain.movie.MovieMetadata;
import com.gcorp.service.app.mvflix_movies.catalog.domain.movie.CatalogItemRepository;
import com.gcorp.service.app.mvflix_movies.catalog.domain.movie.CatalogItemVisibility;
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

    private final CatalogItemRepository movieRepository;
    private final UserProvider userProvider;

    public Mono<CatalogItem> execute(CreateIdentifiedDraftCommand command) {
        CatalogItemVisibility visibility = command.visibility() == null
                ? CatalogItemVisibility.PRIVATE
                : command.visibility();
        List<String> cleanShared = clean(command.sharedWith());
        if (visibility == CatalogItemVisibility.SHARED && cleanShared.isEmpty()) {
            return Mono.error(new IllegalArgumentException(
                    "SHARED requiere al menos un username en usernames"));
        }
        return this.userProvider
                .getAuthenticatedUser()
                .flatMap(user -> {
                    CatalogItem draft = buildIdentifiedDraft(user.subject(), command)
                            .withVisibility(visibility)
                            .withSharedWith(java.util.Set.copyOf(cleanShared));
                    return this.movieRepository.saveDraftWithAccess(draft);
                })
                .doOnNext(saved -> log.info(
                        "Identified draft creado: id={} tmdb={} visibility={} shared={}",
                        saved.getId().value(), saved.isMovie() ? saved.getMovieMetadata().tmdbId() : null,
                        saved.getVisibility(), saved.getSharedWith().size()));
    }

    /**
     * Identidad del proveedor: para MOVIE el tmdbId es obligatorio (el flujo
     * guiado nace de un candidato); VIDEO no se identifica con TMDB.
     */
    private CatalogItem buildIdentifiedDraft(String owner, CreateIdentifiedDraftCommand command) {
        MediaKind kind = command.kind() == null ? MediaKind.MOVIE : command.kind();
        CatalogItem draft = CatalogItem.createDraft(owner, command.metadata(), kind);
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
