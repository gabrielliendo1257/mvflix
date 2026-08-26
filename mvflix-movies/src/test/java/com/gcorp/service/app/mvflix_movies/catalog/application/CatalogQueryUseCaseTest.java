package com.gcorp.service.app.mvflix_movies.catalog.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.gcorp.service.app.mvflix_movies.shared.application.security.AuthenticatedUser;
import com.gcorp.service.app.mvflix_movies.shared.application.security.UserProvider;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import reactor.core.publisher.Mono;

/**
 * La normalización vive aquí y los integration tests construyen
 * CatalogReadQuery directamente, así que ESTE test es el que impide
 * regresiones como dirección invertida o filtros sin traducir.
 */
@ExtendWith(MockitoExtension.class)
class CatalogQueryUseCaseTest {

    @Mock private CatalogViewRepository viewRepository;
    @Mock private UserProvider userProvider;

    private CatalogQueryUseCase useCase;

    @BeforeEach
    void setUp() {
        this.useCase = new CatalogQueryUseCase(this.viewRepository, this.userProvider);
        when(this.userProvider.getAuthenticatedUser())
                .thenReturn(Mono.just(new AuthenticatedUser("pepe", "pepe@m")));
        when(this.viewRepository.page(any())).thenReturn(Mono.empty());
    }

    private CatalogReadQuery captured() {
        ArgumentCaptor<CatalogReadQuery> captor =
                ArgumentCaptor.forClass(CatalogReadQuery.class);
        verify(this.viewRepository, org.mockito.Mockito.atLeastOnce())
                .page(captor.capture());
        return captor.getValue();
    }

    private CatalogReadQuery lastCaptured() {
        ArgumentCaptor<CatalogReadQuery> captor =
                ArgumentCaptor.forClass(CatalogReadQuery.class);
        verify(this.viewRepository, org.mockito.Mockito.atLeastOnce())
                .page(captor.capture());
        var values = captor.getAllValues();
        return values.get(values.size() - 1);
    }

    @Test
    void noDirectionMeansDescendingByDefault() {
        this.useCase.execute(null, null, null, null, null, null).block();

        assertThat(captured().ascending()).isFalse();
        assertThat(captured().sort()).isEqualTo(CatalogReadQuery.SortField.UPDATED_AT);
    }

    @Test
    void ascRisesAndDescFallsRegardlessOfCase() {
        this.useCase.execute(null, null, null, null, "title", "ASC").block();
        assertThat(this.lastCaptured().ascending()).isTrue();

        this.useCase.execute(null, null, null, null, "title", "Asc").block();
        assertThat(this.lastCaptured().ascending()).isTrue();

        this.useCase.execute(null, null, null, null, "title", "desc").block();
        assertThat(this.lastCaptured().ascending()).isFalse();
    }

    @Test
    void statusFilterUsesOperationalVocabulary() {
        // El filtro compara contra display_status: PROCESSING es el valor que
        // la UI ve; DRAFT se acepta como alias legacy.
        this.useCase.execute(null, null, null, "PROCESSING", null, null).block();
        assertThat(this.lastCaptured().status()).isEqualTo("PROCESSING");

        this.useCase.execute(null, null, null, "draft", null, null).block();
        assertThat(this.lastCaptured().status()).isEqualTo("PROCESSING");

        this.useCase.execute(null, null, null, "missing", null, null).block();
        assertThat(this.lastCaptured().status()).isEqualTo("MISSING");
    }

    @Test
    void unknownStatusIsDroppedInsteadOfReturningZeroRows() {
        this.useCase.execute(null, null, null, "no-existe", null, null).block();
        assertThat(this.lastCaptured().status()).isNull();
    }

    @Test
    void blankFiltersBecomeNullAndSearchIsTrimmed() {
        this.useCase.execute(null, null, "  cora ", "  ", null, null).block();

        var query = captured();
        assertThat(query.search()).isEqualTo("cora");
        assertThat(query.status()).isNull();
    }

    @Test
    void pageFloorAndSizeCeiling() {
        this.useCase.execute(-5, 5000, null, null, null, null).block();

        var query = captured();
        assertThat(query.page()).isZero();
        assertThat(query.size()).isEqualTo(CatalogQueryUseCase.MAX_SIZE);
    }

    @Test
    void defaultsForMissingPagination() {
        this.useCase.execute(null, null, null, null, null, null).block();

        var query = captured();
        assertThat(query.page()).isZero();
        assertThat(query.size()).isEqualTo(CatalogQueryUseCase.DEFAULT_SIZE);
    }
}
