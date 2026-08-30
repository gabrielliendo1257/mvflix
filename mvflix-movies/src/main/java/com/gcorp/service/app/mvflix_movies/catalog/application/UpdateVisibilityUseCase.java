package com.gcorp.service.app.mvflix_movies.catalog.application;

import com.gcorp.service.app.mvflix_movies.shared.application.security.UserProvider;
import com.gcorp.service.app.mvflix_movies.catalog.domain.item.CatalogItem;
import com.gcorp.service.app.mvflix_movies.catalog.domain.item.CatalogItemAccessDeniedException;
import com.gcorp.service.app.mvflix_movies.catalog.domain.item.CatalogItemId;
import com.gcorp.service.app.mvflix_movies.catalog.domain.item.CatalogItemRepository;
import com.gcorp.service.app.mvflix_movies.catalog.domain.access.Visibility;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;

import reactor.core.publisher.Mono;

/**
 * Cambia la visibilidad (PUBLIC/PRIVATE/SHARED) de una pelicula del catalogo.
 * Solo el dueño (lo decide {@link CatalogItem#isOwnedBy(String)}); el resto ve 403.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UpdateVisibilityUseCase {

    private final CatalogItemRepository movieRepository;
    private final UserProvider userProvider;

    public Mono<CatalogItem> execute(CatalogItemId id, Visibility visibility) {
        return this.userProvider
                .getAuthenticatedUser()
                .flatMap(user -> this.movieRepository
                        .findById(id)
                        .switchIfEmpty(Mono.error(new CatalogItemAccessDeniedException(
                                "Movie not accessible: " + id.value())))
                        .filter(movie -> movie.isOwnedBy(user.subject()))
                        .switchIfEmpty(Mono.error(new CatalogItemAccessDeniedException(
                                "CatalogItem not owned: " + id.value())))
                        .map(movie -> movie.withVisibility(visibility))
                        .flatMap(this.movieRepository::updateVisibility)
                        .doOnNext(updated -> log.info(
                                "CatalogItem {} visibilidad {} -> {}",
                                id.value(), visibility, updated.getVisibility())));
    }

}
