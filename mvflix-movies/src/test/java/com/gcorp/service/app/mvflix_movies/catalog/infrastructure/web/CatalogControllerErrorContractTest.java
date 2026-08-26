package com.gcorp.service.app.mvflix_movies.catalog.infrastructure.web;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

import com.gcorp.service.app.mvflix_movies.catalog.application.BulkVisibilityUseCase;
import com.gcorp.service.app.mvflix_movies.catalog.application.CatalogQueryUseCase;
import com.gcorp.service.app.mvflix_movies.catalog.application.CatalogViewRepository;
import com.gcorp.service.app.mvflix_movies.catalog.application.CompleteMovieUseCase;
import com.gcorp.service.app.mvflix_movies.catalog.application.CreateIdentifiedDraftUseCase;
import com.gcorp.service.app.mvflix_movies.catalog.application.CreateMovieUseCase;
import com.gcorp.service.app.mvflix_movies.catalog.application.DeleteMovieUseCase;
import com.gcorp.service.app.mvflix_movies.catalog.application.EnrichMovieUseCase;
import com.gcorp.service.app.mvflix_movies.catalog.application.GetMovieUseCase;
import com.gcorp.service.app.mvflix_movies.catalog.application.ListMoviesUseCase;
import com.gcorp.service.app.mvflix_movies.catalog.application.UpdateMovieUseCase;
import com.gcorp.service.app.mvflix_movies.catalog.application.UpdateSharesUseCase;
import com.gcorp.service.app.mvflix_movies.catalog.application.UpdateVisibilityUseCase;
import com.gcorp.service.app.mvflix_movies.infrastructure.web.error.GlobalExceptionHandler;
import com.gcorp.service.app.mvflix_movies.shared.application.security.UserProvider;

import org.junit.jupiter.api.Test;
import org.springframework.test.web.reactive.server.WebTestClient;

class CatalogControllerErrorContractTest {

    private final CatalogViewRepository viewRepository = mock(CatalogViewRepository.class);
    private final UserProvider userProvider = mock(UserProvider.class);

    private final WebTestClient client = WebTestClient
            .bindToController(new MovieController(
                    mock(CreateMovieUseCase.class),
                    mock(CreateIdentifiedDraftUseCase.class),
                    mock(GetMovieUseCase.class),
                    mock(ListMoviesUseCase.class),
                    new CatalogQueryUseCase(this.viewRepository, this.userProvider),
                    mock(UpdateVisibilityUseCase.class),
                    mock(UpdateSharesUseCase.class),
                    mock(BulkVisibilityUseCase.class),
                    mock(UpdateMovieUseCase.class),
                    mock(CompleteMovieUseCase.class),
                    mock(DeleteMovieUseCase.class),
                    mock(EnrichMovieUseCase.class),
                    mock(MovieApiMapper.class)))
            .controllerAdvice(new GlobalExceptionHandler())
            .build();

    @Test
    void invalidStatusReturnsStableBadRequestContractBeforeAccessingDependencies() {
        this.client.get()
                .uri("/api/v1/movies/catalog?status=no-existe")
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.code").isEqualTo(400)
                .jsonPath("$.error").isEqualTo("INVALID_CATALOG_STATUS")
                .jsonPath("$.message").isEqualTo("Unknown catalog status filter: no-existe");

        verifyNoInteractions(this.userProvider, this.viewRepository);
    }
}
