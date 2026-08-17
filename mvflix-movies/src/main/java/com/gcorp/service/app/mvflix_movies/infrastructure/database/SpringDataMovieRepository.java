package com.gcorp.service.app.mvflix_movies.infrastructure.database;

import com.gcorp.service.app.mvflix_movies.domain.movie.Movie;
import com.gcorp.service.app.mvflix_movies.domain.movie.MovieRepository;

import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Repository;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;

@Repository
public class SpringDataMovieRepository implements MovieRepository {

    private final DatabaseClient databaseClient;
    private final MovieRowMapper rowMapper;

    public SpringDataMovieRepository(DatabaseClient databaseClient, MovieRowMapper rowMapper) {
        this.databaseClient = databaseClient;
        this.rowMapper = rowMapper;
    }

    @Override
    public Mono<Movie> save(Movie movie) {
        MovieRow row = this.rowMapper.toRow(movie);
        DatabaseClient.GenericExecuteSpec spec =
                this.databaseClient
                        .sql(
                                """
                                INSERT INTO movies (owner_username, title, status, object_id, object_key, metadata)
                                VALUES (:owner_username, :title, :status, :object_id, :object_key, CAST(:metadata AS jsonb))
                                RETURNING id, owner_username, title, status, object_id, object_key, metadata::text
                                """)
                        .bind("owner_username", row.ownerUsername())
                        .bind("title", row.title())
                        .bind("status", row.status())
                        .bind("metadata", row.metadata());
        if (row.objectId() != null) {
            spec = spec.bind("object_id", row.objectId());
        } else {
            spec = spec.bindNull("object_id", Long.class);
        }
        if (row.objectKey() != null) {
            spec = spec.bind("object_key", row.objectKey());
        } else {
            spec = spec.bindNull("object_key", String.class);
        }
        return spec
                .map(this::toRow)
                .one()
                .map(this.rowMapper::toDomain);
    }

    @Override
    public Mono<Movie> findById(Long id) {
        return this.databaseClient
                .sql(
                        """
                        SELECT id, owner_username, title, status, object_id, object_key, metadata::text
                        FROM movies
                        WHERE id = :id
                        """)
                .bind("id", id)
                .map(this::toRow)
                .one()
                .map(this.rowMapper::toDomain);
    }

    @Override
    public Flux<Movie> findByOwner(String ownerUsername, int limit) {
        return this.databaseClient
                .sql(
                        """
                        SELECT id, owner_username, title, status, object_id, object_key, metadata::text
                        FROM movies
                        WHERE owner_username = :owner_username
                        ORDER BY id DESC
                        LIMIT :limit
                        """)
                .bind("owner_username", ownerUsername)
                .bind("limit", limit)
                .map(this::toRow)
                .all()
                .map(this.rowMapper::toDomain);
    }

    @Override
    public Mono<Movie> completeIfDraft(Long id, String ownerUsername, Long objectId, String objectKey) {
        return this.databaseClient
                .sql(
                        """
                        UPDATE movies
                        SET status = 'READY', object_id = :object_id, object_key = :object_key, updated_at = NOW()
                        WHERE id = :id AND owner_username = :owner_username AND status = 'DRAFT'
                        RETURNING id, owner_username, title, status, object_id, object_key, metadata::text
                        """)
                .bind("id", id)
                .bind("owner_username", ownerUsername)
                .bind("object_id", objectId)
                .bind("object_key", objectKey)
                .map(this::toRow)
                .one()
                .map(this.rowMapper::toDomain);
    }

    @Override
    public Mono<Boolean> deleteById(Long id, String ownerUsername) {
        return this.databaseClient
                .sql(
                        """
                        DELETE FROM movies
                        WHERE id = :id AND owner_username = :owner_username
                        """)
                .bind("id", id)
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

    private MovieRow toRow(io.r2dbc.spi.Row row, io.r2dbc.spi.RowMetadata ignored) {
        return new MovieRow(
            row.get("id", Long.class),
            row.get("owner_username", String.class),
            row.get("title", String.class),
            row.get("status", String.class),
            row.get("object_id", Long.class),
            row.get("object_key", String.class),
            row.get("metadata", String.class));
    }
}
