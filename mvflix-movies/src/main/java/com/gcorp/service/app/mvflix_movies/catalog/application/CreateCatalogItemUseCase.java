package com.gcorp.service.app.mvflix_movies.catalog.application;

import com.gcorp.service.app.mvflix_movies.shared.application.security.UserProvider;
import com.gcorp.service.app.mvflix_movies.catalog.domain.movie.CatalogItem;
import com.gcorp.service.app.mvflix_movies.catalog.domain.movie.CatalogItemRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;

import reactor.core.publisher.Mono;

@Slf4j
@Service
@RequiredArgsConstructor
public class CreateCatalogItemUseCase {

    private final CatalogItemRepository movieRepository;
    private final UserProvider userProvider;

    public Mono<CatalogItem> execute(CreateCatalogItemCommand command) {
        return this.userProvider
                .getAuthenticatedUser()
                .doOnNext(user -> log.info("Creando pelicula en DRAFT: owner={} title={}",
                        user.subject(), command.metadata().title()))
                .flatMap(user -> this.movieRepository.save(
                        CatalogItem.createDraft(user.subject(), command.metadata(), command.kind())))
                .doOnNext(movie -> log.info("Pelicula creada: id={} owner={}", movie.getId(),
                        movie.getOwnerUsername()));
    }
}
