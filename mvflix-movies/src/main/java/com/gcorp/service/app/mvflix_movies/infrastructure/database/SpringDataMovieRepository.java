package com.gcorp.service.app.mvflix_movies.infrastructure.database;

import com.gcorp.service.app.mvflix_movies.domain.movie.Movie;
import com.gcorp.service.app.mvflix_movies.domain.movie.MovieId;
import com.gcorp.service.app.mvflix_movies.domain.movie.MovieRepository;

import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Repository;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;

@Repository
public class SpringDataMovieRepository implements MovieRepository {

    private static final String MEDIA_OBJECT_ID =
            """
            (SELECT mm.object_id FROM media mm
             WHERE mm.movie_id = m.id ORDER BY mm.id LIMIT 1) AS object_id
            """;

    private final DatabaseClient databaseClient;
    private final MovieRowMapper rowMapper;

    public SpringDataMovieRepository(DatabaseClient databaseClient, MovieRowMapper rowMapper) {
        this.databaseClient = databaseClient;
        this.rowMapper = rowMapper;
    }

    @Override
    public Mono<Movie> save(Movie movie) {
        MovieRow row = this.rowMapper.toRow(movie);
        return this.databaseClient
                        .sql(
                                """
                                INSERT INTO movies (owner_username, title, status, enrichment_status, metadata)
                                VALUES (:owner_username, :title, :status, :enrichment_status, CAST(:metadata AS jsonb))
                                RETURNING id, owner_username, title, status, enrichment_status, metadata::text
                                """)
                        .bind("owner_username", row.ownerUsername())
                        .bind("title", row.title())
                        .bind("status", row.status())
                        .bind("enrichment_status", row.enrichmentStatus())
                        .bind("metadata", row.metadata())
                .map(this::toRow)
                .one()
                .map(this.rowMapper::toDomain);
    }

    @Override
    public Mono<Movie> findById(MovieId id) {
        return this.databaseClient
                .sql(
                        """
                        SELECT m.id, m.owner_username, m.title, m.status, m.enrichment_status,
                               m.metadata::text,
                        """
                        + MEDIA_OBJECT_ID
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
    public Flux<Movie> findByOwner(String ownerUsername, int limit) {
        return this.databaseClient
                .sql(
                        """
                        SELECT m.id, m.owner_username, m.title, m.status, m.enrichment_status,
                               m.metadata::text,
                        """
                        + MEDIA_OBJECT_ID
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
    public Mono<Movie> completeIfDraft(MovieId id, String ownerUsername) {
        return this.databaseClient
                .sql(
                        """
                        UPDATE movies
                        SET status = 'READY', updated_at = NOW()
                        WHERE id = :id AND owner_username = :owner_username AND status = 'DRAFT'
                        RETURNING id, owner_username, title, status, enrichment_status, metadata::text
                        """)
                .bind("id", id.value())
                .bind("owner_username", ownerUsername)
                .map(this::toRow)
                .one()
                .map(this.rowMapper::toDomain);
    }

    @Override
    public Mono<Boolean> deleteById(MovieId id, String ownerUsername) {
        return this.databaseClient
                .sql(
                        """
                        DELETE FROM movies
                        WHERE id = :id AND owner_username = :owner_username
                        """)
                .bind("id", id.value())
                .bind("owner_username", ownerUsername)
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

    private MovieRow toRow(io.r2dbc.spi.Row row, io.r2dbc.spi.RowMetadata metadata) {
        return new MovieRow(
            row.get("id", Long.class),
            row.get("owner_username", String.class),
            row.get("title", String.class),
            row.get("status", String.class),
            row.get("enrichment_status", String.class),
            metadata.contains("object_id") ? row.get("object_id", Long.class) : null,
            row.get("metadata", String.class));
    }
}