package com.gcorp.service.app.mvflix_movies.catalog.infrastructure.library;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.gcorp.service.app.mvflix_movies.catalog.domain.movie.CatalogItemId;
import com.gcorp.service.app.mvflix_movies.library.domain.MediaAssetRepository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class LibraryAssetLinksAdapterTest {

    @Mock private MediaAssetRepository mediaAssetRepository;

    @InjectMocks private LibraryAssetLinksAdapter adapter;

    @Test
    void delegatesUnlinkToLibraryRepository() {
        CatalogItemId movieId = CatalogItemId.of(10L);
         when(this.mediaAssetRepository.unlinkByCatalogItemId(
                         com.gcorp.service.app.mvflix_movies.library.domain.CatalogItemId.of(10L)))
                .thenReturn(Mono.just(2L));

        StepVerifier.create(this.adapter.unlinkByCatalogItemId(movieId))
                .expectNext(2L)
                .verifyComplete();

         verify(this.mediaAssetRepository).unlinkByCatalogItemId(
                 com.gcorp.service.app.mvflix_movies.library.domain.CatalogItemId.of(10L));
    }
}
