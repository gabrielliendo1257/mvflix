package com.gcorp.service.app.mvflix_movies.catalog.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.gcorp.service.app.mvflix_movies.shared.application.security.AuthenticatedUser;
import com.gcorp.service.app.mvflix_movies.shared.application.security.UserProvider;
import com.gcorp.service.app.mvflix_movies.catalog.domain.movie.EnrichmentStatus;
import com.gcorp.service.app.mvflix_movies.catalog.domain.movie.MediaKind;
import com.gcorp.service.app.mvflix_movies.catalog.domain.movie.CatalogItem;
import com.gcorp.service.app.mvflix_movies.catalog.domain.movie.CatalogItemAccessDeniedException;
import com.gcorp.service.app.mvflix_movies.catalog.domain.movie.CatalogItemId;
import com.gcorp.service.app.mvflix_movies.catalog.domain.movie.CatalogItemRepository;
import com.gcorp.service.app.mvflix_movies.catalog.domain.movie.CatalogItemStatus;
import com.gcorp.service.app.mvflix_movies.catalog.domain.movie.CatalogItemVisibility;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class UpdateVisibilityUseCaseTest {

    @Mock private CatalogItemRepository movieRepository;
    @Mock private UserProvider userProvider;

    @InjectMocks private UpdateVisibilityUseCase useCase;

    private static CatalogItem movie(long id, String owner, CatalogItemVisibility visibility) {
        return new CatalogItem(
                CatalogItemId.of(id), owner, "Dune", CatalogItemStatus.READY, EnrichmentStatus.ENRICHED,
                null, null, visibility, java.util.Set.of(), MediaKind.MOVIE);
    }

    @Test
    void ownerChangesVisibility() {
        CatalogItem movie = movie(1L, "Javier", CatalogItemVisibility.PRIVATE);
        CatalogItem published = movie(1L, "Javier", CatalogItemVisibility.PUBLIC);

        when(this.userProvider.getAuthenticatedUser())
                .thenReturn(Mono.just(new AuthenticatedUser("Javier", "j@m.com")));
        when(this.movieRepository.findById(CatalogItemId.of(1L)))
                .thenReturn(Mono.just(movie));
        when(this.movieRepository.updateVisibility(any(CatalogItem.class)))
                .thenReturn(Mono.just(published));

        StepVerifier.create(this.useCase.execute(CatalogItemId.of(1L), CatalogItemVisibility.PUBLIC))
                .expectNextMatches(m -> m.getVisibility() == CatalogItemVisibility.PUBLIC)
                .verifyComplete();

        ArgumentCaptor<CatalogItem> captor = ArgumentCaptor.forClass(CatalogItem.class);
        verify(this.movieRepository).updateVisibility(captor.capture());
        assertThat(captor.getValue().getId()).isEqualTo(CatalogItemId.of(1L));
        assertThat(captor.getValue().getVisibility()).isEqualTo(CatalogItemVisibility.PUBLIC);
    }

    @Test
    void nonOwnerIsDenied() {
        CatalogItem movie = movie(1L, "Javier", CatalogItemVisibility.PRIVATE);

        when(this.userProvider.getAuthenticatedUser())
                .thenReturn(Mono.just(new AuthenticatedUser("Maria", "m@m.com")));
        when(this.movieRepository.findById(CatalogItemId.of(1L)))
                .thenReturn(Mono.just(movie));

        StepVerifier.create(this.useCase.execute(CatalogItemId.of(1L), CatalogItemVisibility.PUBLIC))
                .expectError(CatalogItemAccessDeniedException.class)
                .verify();

        verify(this.movieRepository, never()).updateVisibility(any(CatalogItem.class));
    }

    @Test
    void invisibleMovieIsDenied() {
        when(this.userProvider.getAuthenticatedUser())
                .thenReturn(Mono.just(new AuthenticatedUser("Maria", "m@m.com")));
        when(this.movieRepository.findById(CatalogItemId.of(1L)))
                .thenReturn(Mono.empty());

        StepVerifier.create(this.useCase.execute(CatalogItemId.of(1L), CatalogItemVisibility.PUBLIC))
                .expectError(CatalogItemAccessDeniedException.class)
                .verify();

        verify(this.movieRepository, never()).updateVisibility(any(CatalogItem.class));
    }
}
