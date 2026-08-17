package com.gcorp.service.app.mvflix_movies.presenter.api;

import com.gcorp.service.app.mvflix_movies.domain.movie.Movie;
import com.gcorp.service.app.mvflix_movies.domain.movie.MovieMetadata;
import com.gcorp.service.app.mvflix_movies.presenter.api.dto.CreateMovieRequest;
import com.gcorp.service.app.mvflix_movies.presenter.api.dto.MovieResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;

@Mapper(componentModel = "spring")
public interface MovieApiMapper {

    MovieMetadata toMetadata(CreateMovieRequest request);

    @Mappings({
        @Mapping(target = "id", expression = "java(movie.getId().value())"),
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
        @Mapping(target = "awards", source = "metadata.awards")
    })
    MovieResponse toResponse(Movie movie);
}
