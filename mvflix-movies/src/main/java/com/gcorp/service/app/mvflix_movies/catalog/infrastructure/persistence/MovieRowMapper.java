package com.gcorp.service.app.mvflix_movies.catalog.infrastructure.persistence;

import com.gcorp.service.app.mvflix_movies.catalog.domain.movie.EnrichmentStatus;
import com.gcorp.service.app.mvflix_movies.catalog.domain.movie.MediaKind;
import com.gcorp.service.app.mvflix_movies.catalog.domain.movie.Movie;
import com.gcorp.service.app.mvflix_movies.catalog.domain.movie.MovieId;
import com.gcorp.service.app.mvflix_movies.catalog.domain.movie.MovieStatus;
import com.gcorp.service.app.mvflix_movies.catalog.domain.movie.MovieVisibility;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Mapea el agregado de dominio a la fila persistida. La conversión del
 * metadata a JSON la delega en {@link JsonCodec}.
 */
@Mapper(componentModel = "spring", uses = JsonCodec.class)
public interface MovieRowMapper {

    @Mapping(target = "id", expression = "java(movie.getId() == null ? null : movie.getId().value())")
    @Mapping(target = "sharedWith", expression = "java(movie.getSharedWith().toArray(String[]::new))")
    MovieRow toRow(Movie movie);

    @Mapping(target = "id", expression = "java(row.id() == null ? null : MovieId.of(row.id()))")
    @Mapping(target = "status", expression = "java(MovieStatus.valueOf(row.status()))")
    @Mapping(target = "enrichmentStatus", expression = "java(EnrichmentStatus.valueOf(row.enrichmentStatus()))")
    @Mapping(target = "visibility", expression = "java(MovieVisibility.valueOf(row.visibility()))")
    @Mapping(target = "kind", expression = "java(MediaKind.valueOf(row.kind()))")
    @Mapping(target = "sharedWith", expression = "java(row.sharedWith() == null ? java.util.Set.of() : java.util.Set.of(row.sharedWith()))")
    Movie toDomain(MovieRow row);
}