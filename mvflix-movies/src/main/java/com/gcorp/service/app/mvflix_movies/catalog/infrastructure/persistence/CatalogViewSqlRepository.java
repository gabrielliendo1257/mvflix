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
 * SQL read projection del catálogo owned. Dos fuentes UNION ALL:
 *
 * <ol>
 *   <li><b>MEDIA</b>: películas owned con EXISTS de media/media_assets
 *       identificadas (sin JOINs ⇒ una fila por película). display_status
 *       derivado en SQL (READY/PROCESSING/MISSING/ATTENTION).</li>
 *   <li><b>ASSET</b>: archivos de biblioteca SIN identificar (movie_id nulo),
 *       visibles para su descubridor o admin. display_status UNIDENTIFIED,
 *       source LOCAL.</li>
 * </ol>
 * El orden usa whitelist y desempata por key_id; nunca se interpola crudo lo
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
    private static final String MOVIE_ROWS = """
            SELECT 'MEDIA' AS key_type,
                   m.id AS key_id,
                   m.id AS media_id,
                   (SELECT ma.id FROM media_assets ma
                    WHERE ma.movie_id = m.id AND ma.status = 'IDENTIFIED'
                    ORDER BY ma.present DESC, ma.id LIMIT 1) AS asset_id,
                   (SELECT ma.present FROM media_assets ma
                    WHERE ma.movie_id = m.id AND ma.status = 'IDENTIFIED'
                    ORDER BY ma.present DESC, ma.id LIMIT 1) AS asset_present,
                   m.title AS title,
                   COALESCE(m.metadata->>'posterPath', '') AS poster_url,
                   m.metadata->>'year' AS year_text,
                   m.metadata->>'duration' AS duration,
                   m.kind AS kind,
                   m.status AS status,
                   m.display_status AS display_status,
                   CASE WHEN m.has_managed AND m.has_local THEN 'INVALID'
                        WHEN m.has_managed THEN 'MANAGED'
                        WHEN m.has_local THEN 'LOCAL'
                        ELSE 'NONE' END AS source,
                   m.visibility AS visibility,
                   (SELECT COUNT(*) FROM movie_shares ms WHERE ms.movie_id = m.id) AS shared_count,
                   CASE WHEN m.metadata->>'tmdbId' IS NOT NULL THEN 'LINKED' ELSE 'NONE' END AS provider_status,
                   m.updated_at AS updated_at
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

    /**
     * Asset sin identificar: sin movie ni metadata. El título visible es el
     * nombre de archivo. La visibilidad replica la política de ingesta: cada
     * quien ve SUS descubrimientos, el admin ve todos (mirror de
     * MediaAssetQueries.findByLibrary).
     */
    private static final String ASSET_ROWS = """
            SELECT 'ASSET' AS key_type,
                   a.id AS key_id,
                   NULL::bigint AS media_id,
                   a.id AS asset_id,
                   a.present AS asset_present,
                   reverse(split_part(reverse(a.relative_path), '/', 1)) AS title,
                   '' AS poster_url,
                   NULL::text AS year_text,
                   NULL::text AS duration,
                   NULL::text AS kind,
                   NULL::text AS status,
                   CASE WHEN a.present THEN 'UNIDENTIFIED' ELSE 'MISSING' END AS display_status,
                   'LOCAL' AS source,
                   NULL::text AS visibility,
                   0::bigint AS shared_count,
                   NULL::text AS provider_status,
                   a.updated_at AS updated_at
            FROM media_assets a
            WHERE a.movie_id IS NULL
              AND a.status = 'UNIDENTIFIED'
              AND (a.discovered_by = :owner OR :is_admin)
            """;

    private static final String UNION_ALL = MOVIE_ROWS + " UNION ALL " + ASSET_ROWS;

    private static final String ROWS_WRAP = """
            SELECT u.key_type, u.key_id, u.media_id, u.asset_id, u.asset_present,
                   u.title, u.poster_url, u.year_text, u.duration, u.kind, u.status,
                   u.display_status, u.source, u.visibility, u.shared_count, u.provider_status
            FROM (
            """ + UNION_ALL + """
            ) u
            """;

    private static final String STATS_WRAP = """
            SELECT COUNT(*) AS total,
                   COUNT(*) FILTER (WHERE u.display_status = 'READY') AS ready
            FROM (
            """ + UNION_ALL + """
            ) u
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
        String sql = ROWS_WRAP + " WHERE TRUE" + optionalFilters(query)
                + orderClause(query)
                + " LIMIT :size OFFSET :offset";

        GenericExecuteSpec spec = this.databaseClient.sql(sql)
                .bind("owner", query.ownerUsername())
                .bind("is_admin", query.isAdmin())
                .bind("size", query.size())
                .bind("offset", query.offset());
        spec = bindOptionals(spec, query);
        return spec.map((row, meta) -> CatalogRowMappers.toView(row)).all();
    }

    private Mono<Stats> stats(CatalogReadQuery query) {
        String sql = STATS_WRAP + " WHERE TRUE" + optionalFilters(query);
        GenericExecuteSpec spec = this.databaseClient.sql(sql)
                .bind("owner", query.ownerUsername())
                .bind("is_admin", query.isAdmin());
        spec = bindOptionals(spec, query);
        return spec.map((row, meta) -> {
                    long total = row.get("total", Long.class);
                    long ready = row.get("ready", Long.class);
                    return new Stats(total, ready);
                })
                .one();
    }

    /** Filtros sobre la proyección UNION ALL (alias u): título y estado operacional. */
    private static String optionalFilters(CatalogReadQuery query) {
        var filters = new StringBuilder();
        if (query.search() != null) {
            filters.append(" AND LOWER(u.title) LIKE :search");
        }
        if (query.status() != null) {
            filters.append(" AND u.display_status = :status");
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
        // Desempate por key_id: sin él, empates de updated_at/título/año pueden
        // reordenarse entre requests y una página repetiría u omitiría filas.
        return " ORDER BY " + orderColumn(query.sort()) + direction
                + ", u.key_id" + direction;
    }

    private static String orderColumn(CatalogReadQuery.SortField sort) {
        return switch (sort) {
            case TITLE -> "LOWER(u.title)";
            case YEAR -> "COALESCE(u.year_text, '9999')";
            case UPDATED_AT -> "u.updated_at";
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
