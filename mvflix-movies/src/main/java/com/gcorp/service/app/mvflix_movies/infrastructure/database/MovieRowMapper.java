package com.gcorp.service.app.mvflix_movies.infrastructure.database;

import com.gcorp.service.app.mvflix_movies.domain.model.Movie;
import org.mapstruct.Mapper;

/**
 * Mapea el agregado de dominio a la fila persistida. La conversión del
 * metadata a JSON la delega en {@link JsonCodec}.
 */
@Mapper(componentModel = "spring", uses = JsonCodec.class)
public interface MovieRowMapper {

    MovieRow toRow(Movie movie);

    Movie toDomain(MovieRow row);
}
