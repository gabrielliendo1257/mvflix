package com.gcorp.service.app.mvflix_movies.catalog.infrastructure.persistence;

import org.junit.jupiter.api.Test;

import com.gcorp.service.app.mvflix_movies.catalog.application.CatalogReadQuery;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * El desempate por id es lo que impide que páginas consecutivas repitan u
 * omitan filas cuando hay empates de updated_at/título/año.
 */
class CatalogOrderClauseTest {

    @Test
    void defaultSortAppendsIdTiebreakerInSameDirection() {
        var query = new CatalogReadQuery(
                "pepe", 0, 25, null, null,
                CatalogReadQuery.SortField.UPDATED_AT, false);

        assertThat(CatalogViewSqlRepository.orderClause(query))
                .isEqualTo(" ORDER BY m.updated_at DESC, m.id DESC");
    }

    @Test
    void titleSortAscKeepsTiebreakerAscending() {
        var query = new CatalogReadQuery(
                "pepe", 0, 25, null, null,
                CatalogReadQuery.SortField.TITLE, true);

        assertThat(CatalogViewSqlRepository.orderClause(query))
                .isEqualTo(" ORDER BY LOWER(m.title) ASC, m.id ASC");
    }

    @Test
    void yearSortUsesJsonbFallbackForNulls() {
        var query = new CatalogReadQuery(
                "pepe", 0, 25, null, null,
                CatalogReadQuery.SortField.YEAR, false);

        assertThat(CatalogViewSqlRepository.orderClause(query))
                .isEqualTo(" ORDER BY COALESCE(m.metadata->>'year', '9999') DESC, m.id DESC");
    }
}
