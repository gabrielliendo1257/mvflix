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
 * SQL read projection del catálogo owned. Combina de forma controlada:
 * movies (base), media (source MANAGED), media_assets identificadas (source
 * LOCAL), movie_shares (sharedWithCount) y campos de display del JSONB
 * metadata. Paginación honesta: LIMIT/OFFSET en filas y agregados globales
 * para total/summary.
 *
 * <p>El ORDER BY usa whitelist de columnas; nunca se interpola crudo lo que
 * envía el cliente.
 */
@Repository
public class CatalogViewSqlRepository implements CatalogViewRepository {

    /**
     * Fuente y estado operacional derivados en SQL: doble origen es un estado
     * que el catálogo NO oculta (INVALID/ATTENTION, play=false aguas abajo),
     * mismo criterio que playback trata como violación de contrato.
     */
    private static final String ROWS_HEAD = """
            SELECT m.id, m.title, m.status, m.kind, m.visibility,
                   COALESCE(m.metadata->>'posterPath', '') AS poster_url,
                   m.metadata->>'year' AS year_text,
                   m.metadata->>'duration' AS duration,
                   CASE WHEN m.metadata->>'tmdbId' IS NOT NULL THEN 'LINKED' ELSE 'NONE' END AS provider_status,
                   CASE WHEN m.has_managed AND m.has_local THEN 'ATTENTION'
                        WHEN m.status = 'DRAFT' THEN 'PROCESSING'
                        ELSE 'READY' END AS display_status,
                   CASE WHEN m.has_managed AND m.has_local THEN 'INVALID'
                        WHEN m.has_managed THEN 'MANAGED'
                        WHEN m.has_local THEN 'LOCAL'
                        ELSE 'NONE' END AS source,
                   (SELECT ma.id FROM media_assets ma
                    WHERE ma.movie_id = m.id AND ma.status = 'IDENTIFIED'
                    ORDER BY ma.id LIMIT 1) AS asset_id,
                   (SELECT ma.present FROM media_assets ma
                    WHERE ma.movie_id = m.id AND ma.status = 'IDENTIFIED'
                    ORDER BY ma.id LIMIT 1) AS asset_present,
                   (SELECT COUNT(*) FROM movie_shares ms WHERE ms.movie_id = m.id) AS shared_count
            FROM (
                SELECT m0.*, 
                       EXISTS(SELECT 1 FROM media x WHERE x.movie_id = m0.id) AS has_managed,
                       EXISTS(SELECT 1 FROM media_assets x
                              WHERE x.movie_id = m0.id AND x.status = 'IDENTIFIED') AS has_local
                FROM movies m0
                WHERE m0.owner_username = :owner
            """;

    private static final String STATS_HEAD = """
            SELECT COUNT(*) AS total,
                   COUNT(*) FILTER (WHERE m.status = 'READY'
                                    AND NOT (m.has_managed AND m.has_local)) AS ready
            FROM (
                SELECT m0.status,
                       EXISTS(SELECT 1 FROM media x WHERE x.movie_id = m0.id) AS has_managed,
                       EXISTS(SELECT 1 FROM media_assets x
                              WHERE x.movie_id = m0.id AND x.status = 'IDENTIFIED') AS has_local
                FROM movies m0
                WHERE m0.owner_username = :owner
            """;

    // Sin JOINs en el head: los orígenes se resuelven con subconsultas, así una
    // película con varias filas de media/assets aparece EXACTAMENTE una vez y
    // la paginación (LIMIT/OFFSET vs COUNT) permanece consistente.

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
        // ROWS_HEAD ya incluye FROM/WHERE owner; aquí solo filtros opcionales.
        String sql = ROWS_HEAD + innerFilters(query) + "\n ) m"
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
        // STATS_HEAD ya incluye FROM/WHERE owner.
        String sql = STATS_HEAD + innerFilters(query) + "\n ) m";
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

    private static GenericExecuteSpec bindOptionals(
            GenericExecuteSpec spec, CatalogReadQuery query) {
        var bound = query.search() != null
                ? spec.bind("search", "%" + query.search().toLowerCase() + "%")
                : spec;
        return query.status() != null ? bound.bind("status", query.status()) : bound;
    }

    /** Filtros opcionales sobre la tabla interna (m0), no sobre la proyección. */
    private static String innerFilters(CatalogReadQuery query) {
        var filters = new StringBuilder();
        if (query.search() != null) {
            filters.append(" AND LOWER(m0.title) LIKE :search");
        }
        if (query.status() != null) {
            filters.append(" AND m0.status = :status");
        }
        return filters.toString();
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
