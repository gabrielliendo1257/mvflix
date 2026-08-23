package com.gcorp.service.app.mvflix_movies.catalog.infrastructure.library;

import com.gcorp.service.app.mvflix_movies.catalog.application.port.LibraryAssetLinks;
import com.gcorp.service.app.mvflix_movies.catalog.domain.movie.MovieId;
import com.gcorp.service.app.mvflix_movies.library.domain.CatalogItemId;
import com.gcorp.service.app.mvflix_movies.library.domain.MediaAssetRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Component;

import reactor.core.publisher.Mono;

/** Adapter in-process que desvincula assets de Library sin exponer su repositorio a Catalog. */
@Component
@RequiredArgsConstructor
public class LibraryAssetLinksAdapter implements LibraryAssetLinks {

    private final MediaAssetRepository mediaAssetRepository;

    @Override
    public Mono<Long> unlinkByMovieId(MovieId movieId) {
        return this.mediaAssetRepository.unlinkByCatalogItemId(
                CatalogItemId.of(movieId.value()));
    }
}
