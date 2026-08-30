package com.gcorp.service.app.mvflix_movies.catalog.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.gcorp.service.app.mvflix_movies.shared.application.security.AuthenticatedUser;
import com.gcorp.service.app.mvflix_movies.shared.application.security.UserProvider;
import com.gcorp.service.app.mvflix_movies.catalog.domain.movie.EnrichmentStatus;
import com.gcorp.service.app.mvflix_movies.catalog.domain.item.CatalogItemKind;
import com.gcorp.service.app.mvflix_movies.catalog.domain.item.CatalogItem;
import com.gcorp.service.app.mvflix_movies.catalog.domain.item.CatalogItemAccessDeniedException;
import com.gcorp.service.app.mvflix_movies.catalog.domain.item.CatalogItemId;
import com.gcorp.service.app.mvflix_movies.catalog.domain.item.CatalogItemRepository;
import com.gcorp.service.app.mvflix_movies.catalog.domain.item.CatalogItemStatus;
import com.gcorp.service.app.mvflix_movies.catalog.domain.access.Visibility;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.List;
import java.util.Set;

@ExtendWith(MockitoExtension.class)
class UpdateSharesUseCaseTest {

    @Mock private CatalogItemRepository movieRepository;
    @Mock private UserProvider userProvider;

    @InjectMocks private UpdateSharesUseCase useCase;

    private static CatalogItem movie(long id, String owner) {
        return new CatalogItem(
                CatalogItemId.of(id), owner, "Dune", CatalogItemStatus.READY, EnrichmentStatus.ENRICHED,
                null, null, Visibility.SHARED, java.util.Set.of("Maria"), CatalogItemKind.MOVIE);
    }

    @Test
    void ownerReplacesShares() {
        CatalogItem movie = movie(1L, "Javier");

        when(this.userProvider.getAuthenticatedUser())
                .thenReturn(Mono.just(new AuthenticatedUser("Javier", "j@m.com")));
        when(this.movieRepository.findById(CatalogItemId.of(1L)))
                .thenReturn(Mono.just(movie));
        when(this.movieRepository.replaceShares(any(CatalogItem.class)))
                .thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        StepVerifier.create(this.useCase.execute(
                        CatalogItemId.of(1L), List.of("Maria", "Pedro", "Maria", "  ")))
                .expectNextCount(1)
                .verifyComplete();

        ArgumentCaptor<CatalogItem> captor = ArgumentCaptor.forClass(CatalogItem.class);
        verify(this.movieRepository).replaceShares(captor.capture());
        assertThat(captor.getValue().getId()).isEqualTo(CatalogItemId.of(1L));
        assertThat(captor.getValue().getSharedWith())
                .isEqualTo(Set.of("Maria", "Pedro"));
    }

    @Test
    void nonOwnerIsDenied() {
        CatalogItem movie = movie(1L, "Javier");

        when(this.userProvider.getAuthenticatedUser())
                .thenReturn(Mono.just(new AuthenticatedUser("Maria", "m@m.com")));
        when(this.movieRepository.findById(CatalogItemId.of(1L)))
                .thenReturn(Mono.just(movie));

        StepVerifier.create(this.useCase.execute(CatalogItemId.of(1L), List.of("Javier")))
                .expectError(CatalogItemAccessDeniedException.class)
                .verify();

        verify(this.movieRepository, never()).replaceShares(any(CatalogItem.class));
    }
}
