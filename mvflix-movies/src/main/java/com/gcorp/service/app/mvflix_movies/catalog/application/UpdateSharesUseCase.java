package com.gcorp.service.app.mvflix_movies.catalog.application;

import com.gcorp.service.app.mvflix_movies.shared.application.security.UserProvider;
import com.gcorp.service.app.mvflix_movies.catalog.domain.movie.CatalogItem;
import com.gcorp.service.app.mvflix_movies.catalog.domain.movie.CatalogItemAccessDeniedException;
import com.gcorp.service.app.mvflix_movies.catalog.domain.movie.CatalogItemId;
import com.gcorp.service.app.mvflix_movies.catalog.domain.movie.CatalogItemRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;

import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Set;

/**
 * Reemplaza la lista de usuarios con quienes se comparte una pelicula
 * (visibilidad SHARED). Solo el dueño (lo decide {@link CatalogItem#isOwnedBy(String)});
 * el resto ve 403.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UpdateSharesUseCase {

    private final CatalogItemRepository movieRepository;
    private final UserProvider userProvider;

    public Mono<CatalogItem> execute(CatalogItemId id, List<String> usernames) {
        List<String> clean = usernames == null
                ? List.of()
                : usernames.stream()
                        .filter(name -> name != null && !name.isBlank())
                        .distinct()
                        .toList();
        return this.userProvider
                .getAuthenticatedUser()
                .flatMap(user -> this.movieRepository
                        .findById(id)
                        .switchIfEmpty(Mono.error(new CatalogItemAccessDeniedException(
                                "Movie not accessible: " + id.value())))
                        .filter(movie -> movie.isOwnedBy(user.subject()))
                        .switchIfEmpty(Mono.error(new CatalogItemAccessDeniedException(
                                "CatalogItem not owned: " + id.value())))
                        .map(movie -> movie.withSharedWith(Set.copyOf(clean)))
                        .flatMap(this.movieRepository::replaceShares)
                        .doOnNext(updated -> log.info(
                                "CatalogItem {} compartida con {}",
                                id.value(), clean)));
    }
}
