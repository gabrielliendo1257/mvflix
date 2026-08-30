package com.gcorp.service.app.mvflix_movies.catalog.infrastructure.web;

import com.gcorp.service.app.mvflix_movies.catalog.application.UpdateMovieCommand;
import com.gcorp.service.app.mvflix_movies.catalog.application.port.ExternalMovieSearch;
import com.gcorp.service.app.mvflix_movies.catalog.domain.movie.CatalogItem;
import com.gcorp.service.app.mvflix_movies.catalog.domain.movie.MovieMetadata;
import com.gcorp.service.app.mvflix_movies.catalog.infrastructure.web.dto.CreateMovieRequest;
import com.gcorp.service.app.mvflix_movies.catalog.infrastructure.web.dto.EnrichMovieSearchResponse;
import com.gcorp.service.app.mvflix_movies.catalog.infrastructure.web.dto.EnrichmentPreviewResponse;
import com.gcorp.service.app.mvflix_movies.catalog.infrastructure.web.dto.MovieResponse;
import com.gcorp.service.app.mvflix_movies.catalog.infrastructure.web.dto.UpdateMovieRequest;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;

@Mapper(componentModel = "spring")
public interface MovieApiMapper {

    MovieMetadata toMetadata(CreateMovieRequest request);

    UpdateMovieCommand toCommand(UpdateMovieRequest request);

    EnrichMovieSearchResponse toSearchResponse(ExternalMovieSearch search);

    @Mappings({
        @Mapping(target = "title", source = "title"),
        @Mapping(target = "originalTitle", source = "originalTitle"),
        @Mapping(target = "year", source = "year"),
        @Mapping(target = "genres", source = "genres"),
        @Mapping(target = "popularity", source = "popularity"),
        @Mapping(target = "duration", source = "duration"),
        @Mapping(target = "director", source = "director"),
        @Mapping(target = "cast", source = "cast"),
        @Mapping(target = "overview", source = "overview"),
        @Mapping(target = "posterPath", source = "posterPath"),
        @Mapping(target = "releaseDate", source = "releaseDate"),
        @Mapping(target = "country", source = "country"),
        @Mapping(target = "language", source = "language"),
        @Mapping(target = "tmdbId", source = "tmdbId")
    })
    EnrichmentPreviewResponse toPreviewResponse(MovieMetadata metadata);

    @Mappings({
        @Mapping(target = "id", expression = "java(movie.getId().value())"),
        @Mapping(target = "kind", source = "kind"),
         @Mapping(target = "title", source = "movieMetadataOrNull.title"),
         @Mapping(target = "originalTitle", source = "movieMetadataOrNull.originalTitle"),
         @Mapping(target = "year", source = "movieMetadataOrNull.year"),
         @Mapping(target = "genres", source = "movieMetadataOrNull.genres"),
         @Mapping(target = "popularity", source = "movieMetadataOrNull.popularity"),
         @Mapping(target = "duration", source = "movieMetadataOrNull.duration"),
         @Mapping(target = "director", source = "movieMetadataOrNull.director"),
         @Mapping(target = "cast", source = "movieMetadataOrNull.cast"),
         @Mapping(target = "overview", source = "movieMetadataOrNull.overview"),
         @Mapping(target = "posterPath", source = "movieMetadataOrNull.posterPath"),
         @Mapping(target = "releaseDate", source = "movieMetadataOrNull.releaseDate"),
         @Mapping(target = "country", source = "movieMetadataOrNull.country"),
         @Mapping(target = "language", source = "movieMetadataOrNull.language"),
         @Mapping(target = "awards", source = "movieMetadataOrNull.awards"),
         @Mapping(target = "tmdbId", source = "movieMetadataOrNull.tmdbId")
    })
    MovieResponse toResponse(CatalogItem movie);
}
