package com.gcorp.service.app.mvflix_movies.infrastructure.database;

import com.gcorp.service.app.mvflix_movies.domain.movie.EnrichmentStatus;
import com.gcorp.service.app.mvflix_movies.domain.movie.Movie;
import com.gcorp.service.app.mvflix_movies.domain.movie.MovieId;
import com.gcorp.service.app.mvflix_movies.domain.movie.MovieStatus;
import com.gcorp.service.app.mvflix_movies.domain.movie.MovieVisibility;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Mapea el agregado de dominio a la fila persistida. La conversión del
 * metadata a JSON la delega en {@link JsonCodec}.
 */
@Mapper(componentModel = "spring", uses = JsonCodec.class)
public interface MovieRowMapper {

    @Mapping(target = "id", expression = "java(movie.getId() == null ? null : movie.getId().value())")
    MovieRow toRow(Movie movie);

    @Mapping(target = "id", expression = "java(row.id() == null ? null : MovieId.of(row.id()))")
    @Mapping(target = "status", expression = "java(MovieStatus.valueOf(row.status()))")
    @Mapping(target = "enrichmentStatus", expression = "java(EnrichmentStatus.valueOf(row.enrichmentStatus()))")
    @Mapping(target = "visibility", expression = "java(MovieVisibility.valueOf(row.visibility()))")
    Movie toDomain(MovieRow row);
}