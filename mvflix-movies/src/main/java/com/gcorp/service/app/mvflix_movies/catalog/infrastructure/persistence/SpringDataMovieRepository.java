package com.gcorp.service.app.mvflix_movies.catalog.infrastructure.persistence;

import com.gcorp.service.app.mvflix_movies.catalog.domain.movie.EnrichmentStatus;
import com.gcorp.service.app.mvflix_movies.catalog.domain.movie.Movie;
import com.gcorp.service.app.mvflix_movies.catalog.domain.movie.MovieId;
import com.gcorp.service.app.mvflix_movies.catalog.domain.movie.MovieRepository;
import com.gcorp.service.app.mvflix_movies.catalog.domain.movie.MovieVisibility;

import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Repository;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.List;

@Repository
public class SpringDataMovieRepository implements MovieRepository {

    private static final String MEDIA_OBJECT_ID =
            """
            (SELECT mm.object_id FROM media mm
             WHERE mm.movie_id = m.id ORDER BY mm.id LIMIT 1) AS object_id
            """;

    private static final String SHARED_WITH =
            """
            COALESCE((SELECT array_agg(ms.shared_with ORDER BY ms.shared_with)
                      FROM movie_shares ms WHERE ms.movie_id = m.id), ARRAY[]::varchar[]) AS shared_with
            """;

    /**
     * Traducción SQL de {@code Movie.isVisibleTo(username)}: la política de acceso
     * se decide en el dominio; acá solo se filtra en el origen para no traer todo
     * el catálogo a memoria.
     */
    private static final String VISIBLE_WHERE =
            """
            WHERE m.visibility = 'PUBLIC'
               OR m.owner_username = :username
               OR (m.visibility = 'SHARED' AND EXISTS (
                   SELECT 1 FROM movie_shares ms
                   WHERE ms.movie_id = m.id AND ms.shared_with = :username))
            """;

    private static final String SELECT_MOVIE_COLUMNS =
            """
            SELECT m.id, m.owner_username, m.title, m.status, m.enrichment_status,
                   m.metadata::text, m.visibility, m.kind,
            """ + MEDIA_OBJECT_ID + ", " + SHARED_WITH;

    private final DatabaseClient databaseClient;
    private final MovieRowMapper rowMapper;
    private final JsonCodec jsonCodec;

    public SpringDataMovieRepository(
            DatabaseClient databaseClient, MovieRowMapper rowMapper, JsonCodec jsonCodec) {
        this.databaseClient = databaseClient;
        this.rowMapper = rowMapper;
        this.jsonCodec = jsonCodec;
    }

    @Override
    public Mono<Movie> save(Movie movie) {
        MovieRow row = this.rowMapper.toRow(movie);
        return this.databaseClient
                        .sql(
                                """
                                INSERT INTO movies (owner_username, title, status, enrichment_status, metadata, visibility, kind)
                                VALUES (:owner_username, :title, :status, :enrichment_status, CAST(:metadata AS jsonb), :visibility, :kind)
                                RETURNING id, owner_username, title, status, enrichment_status, metadata::text, visibility, kind
                                """)
                        .bind("owner_username", row.ownerUsername())
                        .bind("title", row.title())
                        .bind("status", row.status())
                        .bind("enrichment_status", row.enrichmentStatus())
                        .bind("metadata", row.metadata())
                        .bind("visibility", row.visibility())
                        .bind("kind", row.kind())
                .map(this::toRow)
                .one()
                .map(this.rowMapper::toDomain);
    }

    @Override
    @org.springframework.transaction.annotation.Transactional("connectionFactoryTransactionManager")
    public Mono<Movie> saveDraftWithAccess(Movie movie) {
        // save() asigna el id generado (INSERT RETURNING); replaceShares
        // necesita ese id para insertar en movie_shares. Se propaga el
        // agregado guardado conservando los shares previstos del original.
        return this.save(movie)
            .flatMap(saved -> this.replaceShares(
                new Movie(
                    saved.getId(),
                    saved.getOwnerUsername(),
                    saved.getTitle(),
                    saved.getStatus(),
                    saved.getEnrichmentStatus(),
                    saved.getObjectId(),
                    saved.getMetadata(),
                    saved.getVisibility(),
                    movie.getSharedWith(),
                    saved.getKind())));
    }

    @Override
    public Mono<Movie> findById(MovieId id) {
        return this.databaseClient
                .sql(
                        SELECT_MOVIE_COLUMNS
                        + """
                        FROM movies m
                        WHERE m.id = :id
                        """)
                .bind("id", id.value())
                .map(this::toRow)
                .one()
                .map(this.rowMapper::toDomain);
    }

    @Override
    public Flux<Movie> findVisibleMovies(String username, int limit) {
        return this.databaseClient
                .sql(
                        SELECT_MOVIE_COLUMNS
                        + """
                        FROM movies m
                        """
                        + VISIBLE_WHERE
                        + """
                        ORDER BY m.id DESC
                        LIMIT :limit
                        """)
                .bind("username", username)
                .bind("limit", limit)
                .map(this::toRow)
                .all()
                .map(this.rowMapper::toDomain);
    }

    @Override
    public Flux<Movie> findByOwner(String ownerUsername, int limit) {
        return this.databaseClient
                .sql(
                        SELECT_MOVIE_COLUMNS
                        + """
                        FROM movies m
                        WHERE m.owner_username = :owner_username
                        ORDER BY m.id DESC
                        LIMIT :limit
                        """)
                .bind("owner_username", ownerUsername)
                .bind("limit", limit)
                .map(this::toRow)
                .all()
                .map(this.rowMapper::toDomain);
    }

    @Override
    public Mono<Movie> completeIfDraft(MovieId id) {
        return this.databaseClient
                .sql(
                        """
                        UPDATE movies
                        SET status = 'READY', updated_at = NOW()
                        WHERE id = :id AND status = 'DRAFT'
                        RETURNING id, owner_username, title, status, enrichment_status,
                                  metadata::text, visibility, kind,
                                  (SELECT array_agg(ms.shared_with ORDER BY ms.shared_with)
                                   FROM movie_shares ms WHERE ms.movie_id = movies.id) AS shared_with
                        """)
                .bind("id", id.value())
                .map(this::toRow)
                .one()
                .map(this.rowMapper::toDomain);
    }

    @Override
    public Mono<Boolean> deleteById(MovieId id) {
        return this.databaseClient
                .sql(
                        """
                        DELETE FROM movies
                        WHERE id = :id
                        """)
                .bind("id", id.value())
                .fetch()
                .rowsUpdated()
                .map(rows -> rows > 0);
    }

    @Override
    public Mono<Long> deleteDraftsCreatedBefore(Instant cutoff) {
        return this.databaseClient
                .sql(
                        """
                        DELETE FROM movies
                        WHERE status = 'DRAFT' AND created_at < :cutoff
                        """)
                .bind("cutoff", cutoff)
                .fetch()
                .rowsUpdated()
                .map(Long::valueOf);
    }

    @Override
    public Mono<Movie> updateEnrichment(Movie movie) {
        return this.updateDetailsState(movie, false);
    }

    @Override
    public Mono<Movie> updateDetails(Movie movie) {
        return this.updateDetailsState(movie, true);
    }

    private Mono<Movie> updateDetailsState(Movie movie, boolean updateKind) {
        String kindAssignment = updateKind ? ", kind = :kind" : "";
        DatabaseClient.GenericExecuteSpec statement = this.databaseClient
                .sql(
                        """
                        UPDATE movies
                        SET title = :title, metadata = CAST(:metadata AS jsonb),
                            enrichment_status = :enrichment_status
                        """
                        + kindAssignment
                        + """
                            , updated_at = NOW()
                        WHERE id = :id
                        RETURNING id, owner_username, title, status, enrichment_status,
                                  metadata::text, visibility, kind,
                                  (SELECT mm.object_id FROM media mm
                                   WHERE mm.movie_id = movies.id ORDER BY mm.id LIMIT 1) AS object_id,
                                  (SELECT array_agg(ms.shared_with ORDER BY ms.shared_with)
                                   FROM movie_shares ms WHERE ms.movie_id = movies.id) AS shared_with
                        """)
                .bind("metadata", this.jsonCodec.encode(movie.getMetadata()))
                .bind("title", movie.getMetadata().title())
                .bind("enrichment_status", movie.getEnrichmentStatus().name())
                .bind("id", movie.getId().value());
        if (updateKind) {
            statement = statement.bind("kind", movie.getKind().name());
        }
        return statement
                .map(this::toRow)
                .one()
                .map(this.rowMapper::toDomain);
    }

    @Override
    public Flux<Movie> findByOwnerAndIds(String ownerUsername, List<MovieId> ids) {
        if (ids == null || ids.isEmpty()) {
            return Flux.empty();
        }
        List<Long> values = ids.stream().map(MovieId::value).toList();
        String in = java.util.stream.IntStream.range(0, values.size())
                .mapToObj(i -> ":id" + i)
                .collect(java.util.stream.Collectors.joining(", "));
        DatabaseClient.GenericExecuteSpec spec = this.databaseClient
                .sql(
                        SELECT_MOVIE_COLUMNS
                        + """
                        FROM movies m
                        WHERE m.owner_username = :owner_username
                          AND m.id IN ("""
                        + in
                        + """
                        )
                        ORDER BY m.id
                        """)
                .bind("owner_username", ownerUsername);
        for (int i = 0; i < values.size(); i++) {
            spec = spec.bind("id" + i, values.get(i));
        }
        return spec
                .map(this::toRow)
                .all()
                .map(this.rowMapper::toDomain);
    }

    @Override
    public Mono<Movie> updateVisibility(Movie movie) {
        return this.databaseClient
                .sql(
                        """
                        UPDATE movies
                        SET visibility = :visibility, updated_at = NOW()
                        WHERE id = :id
                        RETURNING id, owner_username, title, status, enrichment_status,
                                  metadata::text, visibility, kind,
                                  (SELECT mm.object_id FROM media mm
                                   WHERE mm.movie_id = movies.id ORDER BY mm.id LIMIT 1) AS object_id,
                                  (SELECT array_agg(ms.shared_with ORDER BY ms.shared_with)
                                   FROM movie_shares ms WHERE ms.movie_id = movies.id) AS shared_with
                        """)
                .bind("visibility", movie.getVisibility().name())
                .bind("id", movie.getId().value())
                .map(this::toRow)
                .one()
                .map(this.rowMapper::toDomain);
    }

    /**
     * Acceso completo en UNA transacción: visibilidad y compartidos se
     * escriben juntos o no se escriben. Espejo de {@code saveDraftWithAccess}
     * para el camino de actualización.
     */
    @Override
    @org.springframework.transaction.annotation.Transactional("connectionFactoryTransactionManager")
    public Mono<Movie> updateAccess(Movie movie) {
        return this.updateVisibility(movie)
            .flatMap(updated -> this.replaceShares(
                new Movie(
                    updated.getId(),
                    updated.getOwnerUsername(),
                    updated.getTitle(),
                    updated.getStatus(),
                    updated.getEnrichmentStatus(),
                    updated.getObjectId(),
                    updated.getMetadata(),
                    updated.getVisibility(),
                    movie.getSharedWith(),
                    updated.getKind())));
    }

    @Override
    @org.springframework.transaction.annotation.Transactional("connectionFactoryTransactionManager")
    public Mono<Movie> replaceShares(Movie movie) {
        return this.databaseClient
                .sql(
                        """
                        DELETE FROM movie_shares
                        WHERE movie_id = :id
                        """)
                .bind("id", movie.getId().value())
                .fetch()
                .rowsUpdated()
                .flatMapMany(deleted ->
                        Flux.fromIterable(movie.getSharedWith())
                            .distinct()
                            .flatMap(username -> this.databaseClient
                                    .sql(
                                            """
                                            INSERT INTO movie_shares (movie_id, shared_with)
                                            SELECT :id, :username
                                            WHERE EXISTS (
                                                SELECT 1 FROM movies m
                                                WHERE m.id = :id)
                                            """)
                                    .bind("id", movie.getId().value())
                                    .bind("username", username)
                                    .fetch()
                                    .rowsUpdated()))
                .then(this.databaseClient
                        .sql(
                                """
                                UPDATE movies
                                SET updated_at = NOW()
                                WHERE id = :id
                                RETURNING id, owner_username, title, status, enrichment_status,
                                          metadata::text, visibility, kind,
                                          (SELECT mm.object_id FROM media mm
                                           WHERE mm.movie_id = movies.id ORDER BY mm.id LIMIT 1) AS object_id,
                                          (SELECT array_agg(ms.shared_with ORDER BY ms.shared_with)
                                           FROM movie_shares ms WHERE ms.movie_id = movies.id) AS shared_with
                                """)
                        .bind("id", movie.getId().value())
                        .map(this::toRow)
                        .one()
                        .map(this.rowMapper::toDomain));
    }

    @Override
    public Flux<Movie> findByEnrichmentStatus(EnrichmentStatus enrichmentStatus, int limit) {
        return this.databaseClient
                .sql(
                        SELECT_MOVIE_COLUMNS
                        + """
                        FROM movies m
                        WHERE m.enrichment_status = :enrichment_status
                        ORDER BY m.id
                        LIMIT :limit
                        """)
                .bind("enrichment_status", enrichmentStatus.name())
                .bind("limit", limit)
                .map(this::toRow)
                .all()
                .map(this.rowMapper::toDomain);
    }

    private MovieRow toRow(io.r2dbc.spi.Row row, io.r2dbc.spi.RowMetadata metadata) {
        return new MovieRow(
            row.get("id", Long.class),
            row.get("owner_username", String.class),
            row.get("title", String.class),
            row.get("status", String.class),
            row.get("enrichment_status", String.class),
            metadata.contains("object_id") ? row.get("object_id", Long.class) : null,
            row.get("metadata", String.class),
            row.get("visibility", String.class),
            metadata.contains("shared_with") ? row.get("shared_with", String[].class) : null,
            row.get("kind", String.class));
    }
}
