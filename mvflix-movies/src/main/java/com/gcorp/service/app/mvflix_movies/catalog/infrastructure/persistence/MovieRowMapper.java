package com.gcorp.service.app.mvflix_movies.catalog.infrastructure.persistence;

import com.gcorp.service.app.mvflix_movies.catalog.domain.movie.EnrichmentStatus;
import com.gcorp.service.app.mvflix_movies.catalog.domain.movie.MediaKind;
import com.gcorp.service.app.mvflix_movies.catalog.domain.movie.CatalogItem;
import com.gcorp.service.app.mvflix_movies.catalog.domain.movie.CatalogItemId;
import com.gcorp.service.app.mvflix_movies.catalog.domain.movie.CatalogItemStatus;
import com.gcorp.service.app.mvflix_movies.catalog.domain.movie.CatalogItemVisibility;
import com.gcorp.service.app.mvflix_movies.catalog.domain.movie.MediaKind;
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
    MovieRow toRow(CatalogItem movie);

    @Mapping(target = "id", expression = "java(row.id() == null ? null : CatalogItemId.of(row.id()))")
    @Mapping(target = "status", expression = "java(CatalogItemStatus.valueOf(row.status()))")
    @Mapping(target = "enrichmentStatus", expression = "java(EnrichmentStatus.valueOf(row.enrichmentStatus()))")
    @Mapping(target = "visibility", expression = "java(CatalogItemVisibility.valueOf(row.visibility()))")
    @Mapping(target = "kind", expression = "java(MediaKind.valueOf(row.kind()))")
    @Mapping(target = "metadata", expression = "java(jsonCodec.decode(row.metadata(), MediaKind.valueOf(row.kind())))")
    @Mapping(target = "sharedWith", expression = "java(row.sharedWith() == null ? java.util.Set.of() : java.util.Set.of(row.sharedWith()))")
    CatalogItem toDomain(MovieRow row);
}
