package com.gcorp.service.app.mvflix_movies.catalog.infrastructure.persistence;

import com.gcorp.service.app.mvflix_movies.catalog.application.CatalogItemView;
import com.gcorp.service.app.mvflix_movies.catalog.application.CatalogPageView;
import com.gcorp.service.app.mvflix_movies.catalog.application.CatalogReadQuery;
import com.gcorp.service.app.mvflix_movies.catalog.application.CatalogViewRepository;

import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.r2dbc.core.DatabaseClient.GenericExecuteSpec;
import org.springframework.stereotype.Repository;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * SQL read projection del catálogo owned. Tres niveles deliberados:
 *
 * <ol>
 *   <li><b>flags</b>: movies con EXISTS de media/media_assets identificadas
 *       (sin JOINs ⇒ una fila por película, paginación consistente).</li>
 *   <li><b>display</b>: UNA sola definición de display_status que gobierna
 *       filas, resumen y filtros (READY/PROCESSING/MISSING/ATTENTION).</li>
 *   <li><b>proyección</b>: columnas de vista y filtros operacionales.</li>
 * </ol>
 * El ORDER BY usa whitelist y desempata por id; nunca se interpola crudo lo
 * que envía el cliente.
 */
@Repository
public class CatalogViewSqlRepository implements CatalogViewRepository {

    /**
     * Una película puede tener varias versiones o calidades locales. Catálogo
     * y playback eligen el asset presente de menor id; esta consulta debe
     * mantenerse alineada con MediaAssetRepository.findByCatalogItemId.
     * PlaybackAndCatalogAssetSelectionConsistencyTest protege esa política.
     */
    private static final String ROWS_SELECT = """
            SELECT m.id, m.title, m.status, m.kind, m.visibility, m.display_status,
                   COALESCE(m.metadata->>'posterPath', '') AS poster_url,
                   m.metadata->>'year' AS year_text,
                   m.metadata->>'duration' AS duration,
                   CASE WHEN m.metadata->>'tmdbId' IS NOT NULL THEN 'LINKED' ELSE 'NONE' END AS provider_status,
                   CASE WHEN m.has_managed AND m.has_local THEN 'INVALID'
                        WHEN m.has_managed THEN 'MANAGED'
                        WHEN m.has_local THEN 'LOCAL'
                        ELSE 'NONE' END AS source,
                   (SELECT ma.id FROM media_assets ma
                    WHERE ma.movie_id = m.id AND ma.status = 'IDENTIFIED'
                    ORDER BY ma.present DESC, ma.id
                    LIMIT 1) AS asset_id,
                   (SELECT ma.present FROM media_assets ma
                    WHERE ma.movie_id = m.id AND ma.status = 'IDENTIFIED'
                    ORDER BY ma.present DESC, ma.id
                    LIMIT 1) AS asset_present,
                   (SELECT COUNT(*) FROM movie_shares ms WHERE ms.movie_id = m.id) AS shared_count
            """;

    private static final String STATS_SELECT = """
            SELECT COUNT(*) AS total,
                   COUNT(*) FILTER (WHERE m.display_status = 'READY') AS ready
            """;

    private static final String FLAGS_AND_DISPLAY = """
            FROM (
                SELECT f.*,
                       CASE WHEN f.has_managed AND f.has_local THEN 'ATTENTION'
                            WHEN NOT f.has_managed AND f.has_local AND NOT f.has_local_ready THEN 'MISSING'
                            WHEN f.status = 'DRAFT' THEN 'PROCESSING'
                            WHEN (f.has_managed OR f.has_local_ready) THEN 'READY'
                            ELSE 'ATTENTION' END AS display_status
                FROM (
                    SELECT m0.*,
                           EXISTS(SELECT 1 FROM media x WHERE x.movie_id = m0.id) AS has_managed,
                           EXISTS(SELECT 1 FROM media_assets x
                                  WHERE x.movie_id = m0.id AND x.status = 'IDENTIFIED') AS has_local,
                           EXISTS(SELECT 1 FROM media_assets x
                                  WHERE x.movie_id = m0.id AND x.status = 'IDENTIFIED'
                                    AND x.present) AS has_local_ready
                    FROM movies m0
                    WHERE m0.owner_username = :owner
                ) f
            ) m
            """;

    private final DatabaseClient databaseClient;

    public CatalogViewSqlRepository(DatabaseClient databaseClient) {
        this.databaseClient = databaseClient;
    }

    @Override
    public Mono<CatalogPageView> page(CatalogReadQuery query) {
        return this.stats(query).flatMap(stats -> this.rows(query)
                .collectList()
                .map(items -> this.assemble(query, items, stats)));
    }

    private Flux<CatalogItemView> rows(CatalogReadQuery query) {
        String sql = ROWS_SELECT + FLAGS_AND_DISPLAY + " WHERE TRUE" + optionalFilters(query)
                + orderClause(query)
                + " LIMIT :size OFFSET :offset";

        GenericExecuteSpec spec = this.databaseClient.sql(sql)
                .bind("owner", query.ownerUsername())
                .bind("size", query.size())
                .bind("offset", query.offset());
        spec = bindOptionals(spec, query);
        return spec.map((row, meta) -> CatalogRowMappers.toView(row)).all();
    }

    private Mono<Stats> stats(CatalogReadQuery query) {
        String sql = STATS_SELECT + FLAGS_AND_DISPLAY + " WHERE TRUE" + optionalFilters(query);
        GenericExecuteSpec spec = this.databaseClient.sql(sql)
                .bind("owner", query.ownerUsername());
        spec = bindOptionals(spec, query);
        return spec.map((row, meta) -> {
                    long total = row.get("total", Long.class);
                    long ready = row.get("ready", Long.class);
                    return new Stats(total, ready);
                })
                .one();
    }

    /** Filtros sobre la proyección: el estado usa el vocabulario operacional. */
    private static String optionalFilters(CatalogReadQuery query) {
        var filters = new StringBuilder();
        if (query.search() != null) {
            filters.append(" AND LOWER(m.title) LIKE :search");
        }
        if (query.status() != null) {
            filters.append(" AND m.display_status = :status");
        }
        return filters.toString();
    }

    private static GenericExecuteSpec bindOptionals(
            GenericExecuteSpec spec, CatalogReadQuery query) {
        var bound = query.search() != null
                ? spec.bind("search", "%" + query.search().toLowerCase() + "%")
                : spec;
        return query.status() != null ? bound.bind("status", query.status()) : bound;
    }

    static String orderClause(CatalogReadQuery query) {
        String direction = query.ascending() ? " ASC" : " DESC";
        // Desempate por id: sin él, empates de updated_at/título/año pueden
        // reordenarse entre requests y una página repetiría u omitiría filas.
        return " ORDER BY " + orderColumn(query.sort()) + direction
                + ", m.id" + direction;
    }

    private static String orderColumn(CatalogReadQuery.SortField sort) {
        return switch (sort) {
            case TITLE -> "LOWER(m.title)";
            case YEAR -> "COALESCE(m.metadata->>'year', '9999')";
            case UPDATED_AT -> "m.updated_at";
        };
    }

    private CatalogPageView assemble(
            CatalogReadQuery query, List<CatalogItemView> items, Stats stats) {
        int totalPages = (int) Math.ceil((double) stats.total() / query.size());
        return new CatalogPageView(
                new CatalogPageView.Summary(
                        stats.total(), stats.ready(), stats.total() - stats.ready()),
                items, query.page(), query.size(), stats.total(), totalPages);
    }

    private record Stats(long total, long ready) {}
}
