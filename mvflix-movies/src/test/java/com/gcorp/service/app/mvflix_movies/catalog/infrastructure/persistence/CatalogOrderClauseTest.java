package com.gcorp.service.app.mvflix_movies.catalog.infrastructure.persistence;

import org.junit.jupiter.api.Test;

import com.gcorp.service.app.mvflix_movies.catalog.application.CatalogReadQuery;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * El desempate por key_id es lo que impide que páginas consecutivas repitan u
 * omitan filas cuando hay empates de updated_at/título/año. El orden opera
 * sobre la proyección UNION ALL (alias u).
 */
class CatalogOrderClauseTest {

    @Test
    void defaultSortAppendsIdTiebreakerInSameDirection() {
        var query = new CatalogReadQuery(
                "pepe", 0, 25, null, null,
                CatalogReadQuery.SortField.UPDATED_AT, false, false);

        assertThat(CatalogViewSqlRepository.orderClause(query))
                .isEqualTo(" ORDER BY u.updated_at DESC, u.key_id DESC");
    }

    @Test
    void titleSortAscKeepsTiebreakerAscending() {
        var query = new CatalogReadQuery(
                "pepe", 0, 25, null, null,
                CatalogReadQuery.SortField.TITLE, true, false);

        assertThat(CatalogViewSqlRepository.orderClause(query))
                .isEqualTo(" ORDER BY LOWER(u.title) ASC, u.key_id ASC");
    }

    @Test
    void yearSortUsesJsonbFallbackForNulls() {
        var query = new CatalogReadQuery(
                "pepe", 0, 25, null, null,
                CatalogReadQuery.SortField.YEAR, false, false);

        assertThat(CatalogViewSqlRepository.orderClause(query))
                .isEqualTo(" ORDER BY COALESCE(u.year_text, '9999') DESC, u.key_id DESC");
    }
}
