package com.gcorp.service.app.mvflix_movies.presenter.api;

import com.gcorp.service.app.mvflix_movies.application.movie.UpdateMovieCommand;
import com.gcorp.service.app.mvflix_movies.domain.enrichment.ExternalMovieSearch;
import com.gcorp.service.app.mvflix_movies.catalog.domain.movie.Movie;
import com.gcorp.service.app.mvflix_movies.catalog.domain.movie.MovieMetadata;
import com.gcorp.service.app.mvflix_movies.presenter.api.dto.CreateMovieRequest;
import com.gcorp.service.app.mvflix_movies.presenter.api.dto.EnrichMovieSearchResponse;
import com.gcorp.service.app.mvflix_movies.presenter.api.dto.EnrichmentPreviewResponse;
import com.gcorp.service.app.mvflix_movies.presenter.api.dto.MovieResponse;
import com.gcorp.service.app.mvflix_movies.presenter.api.dto.UpdateMovieRequest;
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
        @Mapping(target = "title", source = "metadata.title"),
        @Mapping(target = "originalTitle", source = "metadata.originalTitle"),
        @Mapping(target = "year", source = "metadata.year"),
        @Mapping(target = "genres", source = "metadata.genres"),
        @Mapping(target = "popularity", source = "metadata.popularity"),
        @Mapping(target = "duration", source = "metadata.duration"),
        @Mapping(target = "director", source = "metadata.director"),
        @Mapping(target = "cast", source = "metadata.cast"),
        @Mapping(target = "overview", source = "metadata.overview"),
        @Mapping(target = "posterPath", source = "metadata.posterPath"),
        @Mapping(target = "releaseDate", source = "metadata.releaseDate"),
        @Mapping(target = "country", source = "metadata.country"),
        @Mapping(target = "language", source = "metadata.language"),
        @Mapping(target = "awards", source = "metadata.awards"),
        @Mapping(target = "tmdbId", source = "metadata.tmdbId")
    })
    MovieResponse toResponse(Movie movie);
}
